package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.LoginRequest;
import com.emiraslan.memento.dto.LoginResponse;
import com.emiraslan.memento.dto.RegisterRequest;
import com.emiraslan.memento.dto.UserDto;
import com.emiraslan.memento.entity.DoctorProfile;
import com.emiraslan.memento.entity.PatientProfile;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.DoctorProfileRepository;
import com.emiraslan.memento.repository.PatientProfileRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.security.JwtService;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional // rollback all changes if the method fails somewhere
    public UserDto register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("EMAIL_ALREADY_EXISTS");
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

        return MapperUtil.toUserDto(savedUser);
    }

    public LoginResponse login(LoginRequest request) {

        authenticationManager.authenticate( // manager calls provider to authenticate user data
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
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
                .user(MapperUtil.toUserDto(user))
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
}