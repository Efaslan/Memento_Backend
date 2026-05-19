package com.emiraslan.memento.service.auth;

import com.emiraslan.memento.dto.auth.*;
import com.emiraslan.memento.entity.*;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.repository.device.RefreshTokenRepository;
import com.emiraslan.memento.repository.device.UserDeviceRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.service.notification.EmailService;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;
    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Transactional // rollback all changes if the method fails somewhere
    public String register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("EMAIL_ALREADY_EXISTS");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .role(request.getRole())
                .isEmailVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        String verificationToken = generateAndSaveTokenToRedis(savedUser.getEmail());

        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);

        return "REGISTRATION_SUCCESS_CHECK_EMAIL";
    }

    // Token for the verification link email
    private String generateAndSaveTokenToRedis(String email) {
        String token = UUID.randomUUID().toString();
        String redisKey = "email_verify:" + token;

        // email_verify:token : email, link valid for 24 hours
        redisTemplate.opsForValue().set(redisKey, email, Duration.ofHours(24));

        return token;
    }

    @Transactional
    public void verifyEmail(String token) {
        String redisKey = "email_verify:" + token;
        String email = redisTemplate.opsForValue().get(redisKey);

        if (email == null) {
            throw new IllegalArgumentException("INVALID_OR_EXPIRED_TOKEN");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        if (user.getIsEmailVerified()) {
            redisTemplate.delete(redisKey); // delete the unnecessary key
            throw new IllegalStateException("EMAIL_ALREADY_VERIFIED");
        }

        // set user as verified
        user.setIsEmailVerified(true);
        userRepository.save(user);

        // delete the token from redis after it's used
        redisTemplate.delete(redisKey);
    }

    @Transactional
    public int deleteUnverifiedAccounts() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(23);
        return userRepository.deleteUnverifiedUsersOlderThan(cutoff);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate( // manager calls provider to authenticate user data
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                ) // returns BadCredentialsException 401 automatically on fail
        );
        // find the authenticated user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        if (!user.getIsEmailVerified()) {
            throw new AccessDeniedException("EMAIL_NOT_VERIFIED");
        }

        // JWT, Refresh Token and UserDevice are handled by helper method
        return generateAuthTokensForDevice(user, request);
    }

    private LoginResponse generateAuthTokensForDevice(User user, LoginRequest request) {

        UserDevice savedDevice = null;
        Integer requestDeviceId = request.getDeviceId();
        
        // if a deviceId is included in the login request
        if (requestDeviceId != null) {
            // check if the user owns this device
            Optional<UserDevice> existingDeviceOpt = userDeviceRepository.findByDeviceIdAndUser(requestDeviceId, user);

            // if yes, update the devices info
            if (existingDeviceOpt.isPresent()) {
                UserDevice existingDevice = existingDeviceOpt.get();
                existingDevice.setOsVersion(request.getOsVersion());
                existingDevice.setDeviceModel(request.getDeviceModel());
                existingDevice.setLastActive(LocalDateTime.now());

                // save the updated device
                savedDevice = userDeviceRepository.save(existingDevice);
                // delete the device's refresh token because we'll be giving it a new one
                refreshTokenRepository.deleteByDeviceId(savedDevice.getDeviceId());
                // tell Hibernate to immediately execute the deletion and not wait for the end of the Transaction
                refreshTokenRepository.flush();
            }
        }
        // if savedDevice is still null, that means the user logs in for the first time, create them a new device
        if (savedDevice == null) {
            UserDevice newDevice = MapperUtil.toUserDeviceEntity(user, request.getDeviceModel(), request.getOsVersion());
            savedDevice = userDeviceRepository.save(newDevice);
        }

        // --- Creating the JWTs ---

        // generate random JTI for Refresh JWT and hash it
        String jti = UUID.randomUUID().toString();
        String hashedJti = hashJti(jti);

        // create a 14-day refresh token unique for this device and save the hashedJti to DB
        RefreshToken refreshToken = MapperUtil.toRefreshTokenEntity(savedDevice, hashedJti);
        refreshTokenRepository.save(refreshToken);

        // create an Access JWT that is valid for 15 minutes
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getUserId());

        String accessJwt = jwtService.generateAccessToken(extraClaims, user);
        String refreshJwt = jwtService.generateRefreshJwt(user, savedDevice.getDeviceId(), jti); // real JTI goes into user's Refresh JWT

        return LoginResponse.builder()
                .deviceId(savedDevice.getDeviceId())
                .accessJwtToken(accessJwt)
                .refreshToken(refreshJwt)
                .user(MapperUtil.toUserResponseDto(user))
                .build();
    }

    // Refreshing Access Tokens (JWT) with Refresh Tokens
    @Transactional
    public AccessTokenRefreshResponseDto refreshAccessToken(String oldRefreshJwt) {

        // Check if the Refresh JWT has expired or has false signature. (There already is a cleaner CRON that works every day at 00:05 for expired tokens)
        // This check accounts for the small timeframe between 00:00-00:05 that Refresh JWTs might be cancelled after 14 days of not using the app
        jwtService.validateRefreshJwt(oldRefreshJwt);

        // Extract JTI and deviceId from the Refresh JWT
        String incomingJti = jwtService.extractJti(oldRefreshJwt);
        Integer deviceId = jwtService.extractDeviceId(oldRefreshJwt);

        // hash the incoming JTI from Refresh JWT, to compare the token in DBs
        String incomingHashedJti = hashJti(incomingJti);

        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(incomingHashedJti)
                .orElse(null); // not throwing errors because we'll be checking for Refresh JWTs in grace period

        // Check Redis for a Refresh JWT in the grace period
        String redisGracePeriodKey = "refresh_jwt_grace:" + incomingHashedJti;
        boolean isWithinGracePeriod = redisTemplate.hasKey(redisGracePeriodKey);

        // If the Refresh JWT doesn't exist in DB, there are two possibilities:
        if (refreshToken == null){
            // 1. The user has network problems, and they couldn't receive the new Refresh JWT.
            // If the Refresh JWT they are using is in the grace period:
            if (isWithinGracePeriod){
                // find the device's current Refresh JWT in the DB. If it exists, reassign user's Refresh JWT (previous one) to the new one in DB.
                refreshToken = refreshTokenRepository.findByUserDevice_DeviceId(deviceId)
                        .orElseThrow(() -> new EntityNotFoundException("REFRESH_TOKEN_NOT_FOUND"));
            } else {
                // If the Refresh JWT isn't in grace period, that means someone else might have copied the old token.
                // We force logout by deleting the device and deleting all associated tokens (FCM & Refresh JTI) with cascade.
                userDeviceRepository.deleteById(deviceId);

                log.warn("⚠ Potential Refresh JWT THEFT for Device ID: {}. Completely wiped the device to force logout.", deviceId);
                throw new AccessDeniedException("POTENTIAL_REFRESH_JWT_THEFT_DETECTED");
            }
        }

        // if all is ok, get the user from the device
        UserDevice device = refreshToken.getUserDevice();
        User user = device.getUser();

        // generate new JTI for Refresh JWT and hash it
        String newJti = UUID.randomUUID().toString();
        String newHashedJti = hashJti(newJti);

        // save the hashed JTI to DB
        refreshToken.setRefreshToken(newHashedJti);
        refreshTokenRepository.saveAndFlush(refreshToken);

        // update the device's last active info
        device.setLastActive(LocalDateTime.now());
        userDeviceRepository.save(device);

        // save the previous Refresh JWT to Redis for grace period
        redisTemplate.opsForValue().set(redisGracePeriodKey, "oldToken", 5, TimeUnit.MINUTES);
        // not blacklisting the previous Access JWT because it will already be expired when the users use their Refresh JWTs

        // set the claims for new Access Jwt
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getUserId());

        // Generate 2 new JWTs
        String newAccessJwt = jwtService.generateAccessToken(extraClaims, user);
        String newRefreshJwt = jwtService.generateRefreshJwt(user, deviceId, newJti); // We're giving the user's JWT, the real JTI (its hashed in DB)

        return AccessTokenRefreshResponseDto.builder()
                .refreshJwt(newRefreshJwt)
                .accessJwt(newAccessJwt)
                .build();
    }

    private String hashJti(String jti) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256"); // get sha-256 instance
            byte[] hash = digest.digest(jti.getBytes(StandardCharsets.UTF_8)); // turn the JTI into a byte array

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b); // turns the negative valued bytes into 0-255 integers and convert them to hexadecimals
                if (hex.length() == 1) hexString.append('0'); // add 0 if the hex is 1 letter.  a -> 0a
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing Refresh JTI", e);
        }
    }
}