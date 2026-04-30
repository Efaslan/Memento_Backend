package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.auth.LoginRequest;
import com.emiraslan.memento.dto.auth.LoginResponse;
import com.emiraslan.memento.dto.auth.RegisterRequest;
import com.emiraslan.memento.dto.response.UserResponseDto;
import com.emiraslan.memento.entity.DoctorProfile;
import com.emiraslan.memento.entity.PatientProfile;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.DeviceTokenRepository;
import com.emiraslan.memento.repository.DoctorProfileRepository;
import com.emiraslan.memento.repository.PatientProfileRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityExistsException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
    private final DeviceTokenRepository deviceTokenRepository;
    private final StringRedisTemplate redisTemplate;

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
                .orElseThrow();

        // generating token with role and id, useful for frontend
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("role", user.getRole());
        extraClaims.put("userId", user.getUserId());

        String token = jwtService.generateToken(extraClaims, user);

        return LoginResponse.builder()
                .token(token)
                .user(MapperUtil.toUserResponseDto(user))
                .build();
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

    @Transactional
    public void logout(String fcmToken, String jwt){
        deviceTokenRepository.deleteByFcmToken(fcmToken); // delete the fcm token so the user's device doesn't get notifications after logout
        log.info("FCM Token deleted: {}", fcmToken);

        if(jwt != null){
            long expirationTimeMillis = jwtService.getExpirationTime(jwt);
            long currentTimeMillis = System.currentTimeMillis();
            long remainingTimeMillis = expirationTimeMillis - currentTimeMillis;

            if(remainingTimeMillis > 0 ){
                String redisKey = BLACKLIST_PREFIX + jwt;

                redisTemplate.opsForValue().set(redisKey, "val", remainingTimeMillis, TimeUnit.MILLISECONDS);
                log.info("JWT added to Redis blacklist. Remaining time (ms): {}", remainingTimeMillis);
            }
        }
    }
}