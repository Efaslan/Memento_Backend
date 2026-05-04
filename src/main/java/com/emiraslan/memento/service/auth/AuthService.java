package com.emiraslan.memento.service.auth;

import com.emiraslan.memento.dto.auth.LoginRequest;
import com.emiraslan.memento.dto.auth.LoginResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

        // create a 90-day refresh token unique for this device
        RefreshToken refreshToken = MapperUtil.toRefreshTokenEntity(savedDevice);
        refreshTokenRepository.save(refreshToken);

        // create an access token (JWT) that is valid for 1 hour
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getUserId());
        String jwt = jwtService.generateToken(extraClaims, user);

        return LoginResponse.builder()
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

        // Check if the refresh token is expired
        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            // delete the device to invalidate its tokens
            userDeviceRepository.delete(refreshToken.getUserDevice());
            throw new IllegalStateException("REFRESH_TOKEN_EXPIRED");
        }

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

    @Transactional
    public void logout(String refreshTokenString, String jwt){

        // find the device and delete it
        // Refresh and Notification tokens of the device are also deleted through Cascade
        refreshTokenRepository.findByRefreshToken(refreshTokenString).ifPresent(tokenEntity -> {
            userDeviceRepository.delete(tokenEntity.getUserDevice());
            log.info("Device Session and all associated tokens deleted for Device ID: {}", tokenEntity.getUserDevice().getDeviceId());
        });

        // Blacklist the current JWT
        if (jwt != null) {
            long expirationTimeMillis = jwtService.getExpirationTime(jwt);
            long currentTimeMillis = System.currentTimeMillis();
            long remainingTimeMillis = expirationTimeMillis - currentTimeMillis;

            if (remainingTimeMillis > 0) {
                String redisKey = BLACKLIST_PREFIX + jwt;
                redisTemplate.opsForValue().set(redisKey, "val", remainingTimeMillis, TimeUnit.MILLISECONDS);
                log.info("JWT added to Redis blacklist. Remaining time (ms): {}", remainingTimeMillis);
            }
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