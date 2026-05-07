package com.emiraslan.memento.service.auth;

import com.emiraslan.memento.dto.auth.LoginRequest;
import com.emiraslan.memento.dto.auth.LoginResponse;
import com.emiraslan.memento.dto.auth.PasskeyVerifyRequestDto;
import com.emiraslan.memento.dto.auth.RegisterRequest;
import com.emiraslan.memento.dto.response.UserResponseDto;
import com.emiraslan.memento.entity.*;
import com.emiraslan.memento.entity.user.DoctorProfile;
import com.emiraslan.memento.entity.user.PatientProfile;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.device.RefreshTokenRepository;
import com.emiraslan.memento.repository.device.UserDeviceRepository;
import com.emiraslan.memento.repository.user.DoctorProfileRepository;
import com.emiraslan.memento.repository.user.PatientProfileRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final StringRedisTemplate redisTemplate;

    private final UserDeviceRepository userDeviceRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    @Transactional // rollback all changes if the method fails somewhere
    public UserResponseDto register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EntityExistsException("EMAIL_ALREADY_EXISTS");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(request.getRole())
                .build();

        User savedUser = userRepository.save(user);
        createEmptyProfileForRole(savedUser);

        return MapperUtil.toUserResponseDto(savedUser);
    }

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

        // JWT, Refresh Token and UserDevice are handled by helper method
        return generateAuthTokensForDevice(user, request.getDeviceModel(), request.getOsVersion());
    }

    private LoginResponse generateAuthTokensForDevice(User user, String deviceModel, String osVersion) {

        // register new device
        UserDevice device = MapperUtil.toUserDeviceEntity(user, deviceModel, osVersion);
        UserDevice savedDevice = userDeviceRepository.save(device);

        // create a 14-day refresh token unique for this device
        RefreshToken refreshToken = MapperUtil.toRefreshTokenEntity(savedDevice);
        refreshTokenRepository.save(refreshToken);

        // create an access token (JWT) that is valid for 1 hour
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getUserId());
        String jwt = jwtService.generateToken(extraClaims, user);

        return LoginResponse.builder()
                .deviceId(savedDevice.getDeviceId())
                .accessJwtToken(jwt)
                .refreshToken(refreshToken.getRefreshToken())
                .user(MapperUtil.toUserResponseDto(user))
                .build();
    }

    // Refreshing Access Tokens (JWT) with Refresh Tokens
    @Transactional
    public LoginResponse refreshAccessToken(String refreshTokenString) {

        RefreshToken refreshToken = refreshTokenRepository.findByRefreshToken(refreshTokenString)
                .orElseThrow(() -> new IllegalStateException("INVALID_REFRESH_TOKEN"));

        // we auto-delete refresh tokens at the end of days. So there is no need to check if it has expired

        User user = refreshToken.getUserDevice().getUser();

        // update the device's last active info
        refreshToken.getUserDevice().setLastActive(Instant.now());
        userDeviceRepository.save(refreshToken.getUserDevice());

        // generate a new short-lived JWT (refresh token stays the same)
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getUserId());
        String newJwt = jwtService.generateToken(extraClaims, user);

        return LoginResponse.builder()
                .accessJwtToken(newJwt)
                .refreshToken(refreshToken.getRefreshToken())
                .user(MapperUtil.toUserResponseDto(user))
                .build();
    }

    // for passkey verification after RefreshToken expiration. We give the mobile frontend a random text,
    // and receive it back after it gets signed with the private key from Trusted Execution Environment (TEE), or
    // from the disk encrypted with the user's app-special PIN code if biometric data isn't available.
    public String generatePasskeyChallenge(Integer deviceId) {
        // creating a unique challenge text for the device
        String challenge = UUID.randomUUID().toString();

        String redisKey = "passkey:challenge:device:" + deviceId;

        // save the key:challenge to redis with 3 minutes TTL. ONLY save the challenge if the key is ABSENT
        // This helps prevent overwrite attacks while a user is trying to complete the passkey verification challenge
        Boolean isChallengeActive = redisTemplate.opsForValue().setIfAbsent(redisKey, challenge, Duration.ofMinutes(3));

        if (Boolean.FALSE.equals(isChallengeActive)) {
            throw new IllegalStateException("CHALLENGE_ALREADY_ACTIVE_PLEASE_WAIT");
        }

        return challenge;
    }

    @Transactional
    public LoginResponse verifyPasskeyAndLogin(PasskeyVerifyRequestDto requestDto){
        // find the device
        UserDevice device = userDeviceRepository.findById(requestDto.getDeviceId())
                .orElseThrow(() -> new EntityNotFoundException("DEVICE_NOT_FOUND"));

        // can't decode the challenge without a public key
        if (device.getPublicKey() == null) {
            throw new IllegalStateException("PUBLIC_KEY_NOT_REGISTERED_FOR_DEVICE_USE_PASSWORD_FOR_LOGIN");
        }

        // get the challenge text from redis
        String redisKey = "passkey:challenge:device:" + device.getDeviceId();
        String activeChallenge = redisTemplate.opsForValue().get(redisKey);

        if (activeChallenge == null) {
            throw new IllegalStateException("CHALLENGE_EXPIRED_OR_NOT_FOUND");
        }

        // decode the signature with the public key, and see if it equals the challenge text from redis
        boolean isSignatureValid = verifySignature(device.getPublicKey(), activeChallenge, requestDto.getSignature());

        if (!isSignatureValid) {
            throw new BadCredentialsException("INVALID_SIGNATURE");
        }

        // delete redis key and previous expired refresh token from DBs
        redisTemplate.delete(redisKey);
        refreshTokenRepository.deleteByUserDevice_DeviceId(device.getDeviceId());

        // generate a new RefreshToken for the device and save it to DB
        RefreshToken refreshToken = MapperUtil.toRefreshTokenEntity(device);
        refreshTokenRepository.save(refreshToken);

        User user = device.getUser();

        // generate the access JWT
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getUserId());
        String newAccessJwt = jwtService.generateToken(extraClaims, user);

        // update the device's last active info
        device.setLastActive(Instant.now());
        userDeviceRepository.save(device);

        // return device id (mobile already has it), new refresh and JWT access tokens
        return LoginResponse.builder()
                .deviceId(device.getDeviceId())
                .refreshToken(refreshToken.getRefreshToken())
                .accessJwtToken(newAccessJwt)
                .build();
    }

    private boolean verifySignature(String base64PublicKey, String challenge, String base64Signature){
        try {
            // formatting the public key into a byte array
            byte[] publicKeyBytes = Base64.getDecoder().decode(base64PublicKey);

            // we define the bytes as a public key created with x509 standards
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);

            // create a factory that builds RSA keys (our pair is the public key - mobile has the private key)
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            // creating the public key object using our X509 keySpec
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Prepare the Signature machine to verify an SHA-256 hash sealed with an RSA Private Key
            Signature signature = Signature.getInstance("SHA256withRSA");

            // we will use this public key to unseal the signature sent by mobile
            signature.initVerify(publicKey);

            // hash the challenge with SHA256 and find its always 32 byte fingerprint, and keep it in memory for comparison
            signature.update(challenge.getBytes());

            // format the encrypted signature from mobile into a byte array
            byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);

            // 1. unseal signatureBytes using the publicKey
            // 2. compare the unsealed hash with our own hashed copy of the challenge
            // 3. if they match, return true
            return signature.verify(signatureBytes);

        } catch (Exception e){
            log.error("Signature verification FAILED!", e);
            return false;
        }
    }


    // creates empty profiles on register
    private void createEmptyProfileForRole(User user) {
        if (user.getRole() == UserRole.PATIENT) {
            PatientProfile profile = PatientProfile.builder()
                    .patient(user)
                    .build();
            patientProfileRepository.save(profile);
        }
        else if (user.getRole() == UserRole.DOCTOR) {
            DoctorProfile profile = DoctorProfile.builder()
                    .doctor(user)
                    .build();
            doctorProfileRepository.save(profile);
        }
        // no profile for relatives
    }
}