package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.DoctorProfileRequestDto;
import com.emiraslan.memento.dto.request.PatientProfileRequestDto;
import com.emiraslan.memento.dto.response.DoctorProfileResponseDto;
import com.emiraslan.memento.dto.response.PatientProfileResponseDto;
import com.emiraslan.memento.entity.user.DoctorProfile;
import com.emiraslan.memento.entity.user.PatientProfile;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.repository.user.DoctorProfileRepository;
import com.emiraslan.memento.repository.user.PatientProfileRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.service.notification.OtpService;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;

    // PATIENT PROFILE OPERATIONS

    public PatientProfileResponseDto getPatientProfile(Integer patientId) {
        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("PATIENT_PROFILE_NOT_FOUND: " + patientId));

        return MapperUtil.toPatientProfileResponseDto(profile);
    }

    // profiles are created blank on register, so we only update them here
    @Transactional
    public PatientProfileResponseDto upsertPatientProfile(Integer patientId, PatientProfileRequestDto dto) {
        User user = userRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        // bring it if profile exists, or create it
        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseGet(() -> PatientProfile.builder()
                        .patient(user)
                        .build());

        // profile data
        profile.setDateOfBirth(dto.getDateOfBirth());
        profile.setHeightCm(dto.getHeightCm());
        profile.setWeightKg(dto.getWeightKg());
        profile.setBloodType(dto.getBloodType());
        profile.setEmergencyNotes(dto.getEmergencyNotes());

        // user data, no email or role change
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());

        // email is not updated here

        userRepository.save(user);
        PatientProfile updatedProfile = patientProfileRepository.save(profile);

        return MapperUtil.toPatientProfileResponseDto(updatedProfile);
    }

    // DOCTOR PROFILE OPERATIONS

    public DoctorProfileResponseDto getDoctorProfile(Integer doctorId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("DOCTOR_PROFILE_NOT_FOUND: " + doctorId));

        return MapperUtil.toDoctorProfileResponseDto(profile);
    }

    @Transactional
    public DoctorProfileResponseDto upsertDoctorProfile(Integer doctorId, DoctorProfileRequestDto dto) {
        User user = userRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        // bring it if profile exists, or create it
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseGet(() -> DoctorProfile.builder()
                        .doctor(user)
                        .build());

        // profile data
        profile.setSpecialization(dto.getSpecialization());
        profile.setHospitalName(dto.getHospitalName());
        profile.setTitle(dto.getTitle());

        // user data
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());

        // email is not updated here

        userRepository.save(user);
        DoctorProfile updatedProfile = doctorProfileRepository.save(profile);

        return MapperUtil.toDoctorProfileResponseDto(updatedProfile);
    }

    @Transactional
    public void requestEmailChange(Integer userId, String newEmail) {

        if (userRepository.existsByEmail(newEmail)) {
            throw new EntityExistsException("EMAIL_ALREADY_EXISTS");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));

        if (user.getEmail().equalsIgnoreCase(newEmail)) {
            throw new IllegalArgumentException("SAME_EMAIL_ADDRESS");
        }

        // send 6-digit otp to the new email address
        otpService.generateAndSendOtpForEmailChange(user, newEmail);
        log.info("Email change OTP sent for User ID: {}", userId);
    }

    @Transactional
    public void verifyAndChangeEmail(Integer userId, String newEmail, String otpCode) {

        // validate OTP code
        otpService.validateOtpForEmailChange(userId, newEmail, otpCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));


        // update email
        String oldEmail = user.getEmail();
        user.setEmail(newEmail);
        userRepository.save(user);

        log.info("User ID: {} successfully changed email from {} to {}", userId, oldEmail, newEmail);
    }
}