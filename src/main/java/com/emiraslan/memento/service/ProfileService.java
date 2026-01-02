package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.DoctorProfileDto;
import com.emiraslan.memento.dto.PatientProfileDto;
import com.emiraslan.memento.entity.DoctorProfile;
import com.emiraslan.memento.entity.PatientProfile;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.repository.DoctorProfileRepository;
import com.emiraslan.memento.repository.PatientProfileRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final PatientProfileRepository patientProfileRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    // PATIENT PROFILE OPERATIONS

    public PatientProfileDto getPatientProfile(Integer patientId) {
        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("PATIENT_PROFILE_NOT_FOUND: " + patientId));

        return MapperUtil.toPatientProfileDto(profile);
    }

    // profiles are created blank on register, so we only update them here
    @Transactional
    public PatientProfileDto updatePatientProfile(Integer patientId, PatientProfileDto dto) {
        PatientProfile profile = patientProfileRepository.findById(patientId)
                .orElseThrow(() -> new EntityNotFoundException("PATIENT_PROFILE_NOT_FOUND: " + patientId));

        User user = profile.getPatient();

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

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            // if email has changed, and it is in usage by someone else
            if (!dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                // catch and throw a 400 error
                throw new EntityExistsException("EMAIL_ALREADY_EXISTS");
            }
            user.setEmail(dto.getEmail());
        }

        userRepository.save(user);
        PatientProfile updatedProfile = patientProfileRepository.save(profile);

        return MapperUtil.toPatientProfileDto(updatedProfile);
    }

    // DOCTOR PROFILE OPERATIONS

    public DoctorProfileDto getDoctorProfile(Integer doctorId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("DOCTOR_PROFILE_NOT_FOUND: " + doctorId));

        return MapperUtil.toDoctorProfileDto(profile);
    }

    @Transactional
    public DoctorProfileDto updateDoctorProfile(Integer doctorId, DoctorProfileDto dto) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new EntityNotFoundException("DOCTOR_PROFILE_NOT_FOUND: " + doctorId));

        User user = profile.getDoctor();

        // profile data
        profile.setSpecialization(dto.getSpecialization());
        profile.setHospitalName(dto.getHospitalName());
        profile.setTitle(dto.getTitle());

        // user data
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            if (!dto.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(dto.getEmail())) {
                throw new EntityExistsException("EMAIL_ALREADY_EXISTS");
            }
            user.setEmail(dto.getEmail());
        }

        userRepository.save(user);
        DoctorProfile updatedProfile = doctorProfileRepository.save(profile);

        return MapperUtil.toDoctorProfileDto(updatedProfile);
    }
}