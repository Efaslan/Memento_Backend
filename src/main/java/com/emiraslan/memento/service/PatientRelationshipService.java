package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.PatientCardDto;
import com.emiraslan.memento.dto.response.RelationshipResponseDto;
import com.emiraslan.memento.dto.request.RelationshipRequestDto;
import com.emiraslan.memento.entity.user.PatientProfile;
import com.emiraslan.memento.entity.user.PatientRelationship;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.RelationshipType;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.user.PatientProfileRepository;
import com.emiraslan.memento.repository.user.PatientRelationshipRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.service.notification.OtpService;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientRelationshipService {

    private final PatientRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PatientProfileRepository patientProfileRepository;

    @Transactional
    public Slice<PatientCardDto> getDoctorPatients(User doctor, String searchTerm, Pageable pageable){
        Slice<PatientRelationship> relationshipSlice = relationshipRepository
                .findActivePatientsForDoctor(doctor.getUserId(), searchTerm, pageable);

        List<Integer> patientIds = relationshipSlice.getContent().stream()
                .map(rel -> rel.getPatient().getUserId())
                .toList(); // toList() returns an immutable list, only allowing reading

        Map<Integer, PatientProfile> profileMap = patientProfileRepository.findByPatient_UserIdIn(patientIds)
                .stream().
                collect(Collectors.toMap(
                        PatientProfile::getPatientUserId,
                        profile -> profile
                ));

        return relationshipSlice.map(rel -> {
            Integer patientId = rel.getPatient().getUserId();
            PatientProfile profile = profileMap.get(patientId);

            return MapperUtil.toPatientCardDto(rel, profile);
        });
    }

    public List<RelationshipResponseDto> getActiveRelationships(User user) {
        return relationshipRepository.findAllActiveRelationshipsByUserId(user.getUserId())
                .stream()
                .map(MapperUtil::toRelationshipResponseDto)
                .toList();
    }

    @Transactional
    public void relationshipRequestByPatient(String email, User initiator){
        otpService.generateAndSendOtpForRelationshipInvitation(email, initiator);
    }

    @Transactional
    public RelationshipResponseDto addRelationship(RelationshipRequestDto dto, User initiator) {
        if(initiator.getRole() == UserRole.PATIENT){
            return addRelativeByPatient(dto, initiator);
        }
        // if the user is not patient, then they are a doctor. Relatives are not allowed through the endpoint
        return addPatientByDoctor(dto, initiator);
    }

    // case 1: patients can only add other patients or relatives
    private RelationshipResponseDto addRelativeByPatient(RelationshipRequestDto dto, User initiatorPatient) {

        User caregiver = userRepository.findByEmail(dto.getTargetEmail())
                .orElseThrow(() -> new EntityNotFoundException("TARGET_CAREGIVER_NOT_FOUND"));

        // checking the relationship role first to prevent OTP from being deleted early
        if (caregiver.getRole() == UserRole.DOCTOR || dto.getRelationshipType() == RelationshipType.DOCTOR) {
            throw new IllegalArgumentException("PATIENTS_CANNOT_ADD_DOCTORS");
        }

        // OTP is needed for when patients add relationships
        if (dto.getOtpCode() == null || dto.getOtpCode().trim().isEmpty()) {
            throw new IllegalArgumentException("OTP_CODE_IS_REQUIRED_FOR_PATIENTS");
        }
        otpService.validateOtpForRelationshipInvitation(dto.getTargetEmail(), initiatorPatient, dto.getOtpCode());

        Boolean isPrimary = dto.getIsPrimaryContact() != null ? dto.getIsPrimaryContact() : false;

        return createRelationship(initiatorPatient, caregiver, dto.getRelationshipType(), isPrimary);
    }

    // case 2: doctors adding patients
    private RelationshipResponseDto addPatientByDoctor(RelationshipRequestDto dto, User doctor) {
        User patient = userRepository.findByEmail(dto.getTargetEmail())
                .orElseThrow(() -> new EntityNotFoundException("TARGET_PATIENT_NOT_FOUND"));

        if (patient.getRole() != UserRole.PATIENT) {
            throw new IllegalArgumentException("TARGET_USER_IS_NOT_PATIENT");
        }

        // doctor users can also be 'son/daughter etc.' in relationships. If the type is not specified, default is DOCTOR
        RelationshipType type = dto.getRelationshipType() != null ? dto.getRelationshipType() : RelationshipType.DOCTOR;
        Boolean isPrimary = dto.getIsPrimaryContact(); // can be null, it is saved as false in that case

        return createRelationship(patient, doctor, type, isPrimary);
    }

    // common logic for saving/updating relationships
    private RelationshipResponseDto createRelationship(User patient, User caregiver, RelationshipType type, Boolean isPrimaryContact) {

        if (patient.getUserId().equals(caregiver.getUserId())) {
            throw new IllegalArgumentException("SELF_RELATION_NOT_ALLOWED");
        }
        // duplicate check
        Optional<PatientRelationship> existingRel = relationshipRepository
                .findByPatient_UserIdAndCaregiver_UserId(patient.getUserId(), caregiver.getUserId());

        PatientRelationship relationship;

        if (existingRel.isPresent()) {
            relationship = existingRel.get();

            if (Boolean.TRUE.equals(existingRel.get().getIsActive())) {
                throw new IllegalStateException("RELATIONSHIP_ALREADY_EXISTS_AND_ACTIVE");
            }

            // relationship exists but inactive, reactivating it
            relationship.setIsActive(true);
            relationship.setRelationshipType(type);
            relationship.setIsPrimaryContact(isPrimaryContact != null ? isPrimaryContact : false);
        } else {
            // create the relationship if it doesn't exist
            relationship = PatientRelationship.builder()
                .patient(patient)
                .caregiver(caregiver)
                .relationshipType(type)
                .isPrimaryContact(isPrimaryContact != null ? isPrimaryContact : false)
                .isActive(true)
                .build();
        }
        return MapperUtil.toRelationshipResponseDto(relationshipRepository.save(relationship));
    }

    @Transactional
    public RelationshipResponseDto updateRelationship(Integer relationshipId, RelationshipResponseDto dto, User initiator) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

        boolean isCaregiver = relationship.getCaregiver().getUserId().equals(initiator.getUserId());

        // if the relationship is currently not doctor-patient, only the doctor can make it doctor-patient
        if (dto.getRelationshipType() == RelationshipType.DOCTOR) {
            if (!isCaregiver) { // patients cannot edit the type into doctor
                throw new IllegalStateException("ONLY_CAREGIVERS_CAN_SET_TYPE_TO_DOCTOR");
            }
            if (relationship.getCaregiver().getRole() != UserRole.DOCTOR) { // user has to be a doctor to edit the type into doctor
                throw new IllegalArgumentException("CANNOT_SET_TYPE_TO_DOCTOR_IF_USER_IS_NOT_DOCTOR");
            }
        }

        if (dto.getRelationshipType() != null) relationship.setRelationshipType(dto.getRelationshipType());
        if (dto.getIsPrimaryContact() != null) relationship.setIsPrimaryContact(dto.getIsPrimaryContact());

        return MapperUtil.toRelationshipResponseDto(relationshipRepository.save(relationship));
    }

    // toggle to change primary contacts
    @Transactional
    public RelationshipResponseDto togglePrimaryContactStatus(Integer relationshipId) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

        // null check, if bool is null, it becomes false
        boolean currentStatus = Boolean.TRUE.equals(relationship.getIsPrimaryContact());
        relationship.setIsPrimaryContact(!currentStatus); // reversing primary contact status

        return MapperUtil.toRelationshipResponseDto(relationshipRepository.save(relationship));
    }
}
