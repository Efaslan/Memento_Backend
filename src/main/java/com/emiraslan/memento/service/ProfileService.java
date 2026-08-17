package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.request.PatientProfileRequestDto;
import com.emiraslan.memento.dto.response.PatientProfileResponseDto;
import com.emiraslan.memento.entity.user.PatientProfile;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.repository.user.PatientProfileRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final UserRepository userRepository;

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
}