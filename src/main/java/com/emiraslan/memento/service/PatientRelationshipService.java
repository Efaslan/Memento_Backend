package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.PatientRelationshipDto;
import com.emiraslan.memento.entity.PatientRelationship;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.RelationshipType;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.PatientRelationshipRepository;
import com.emiraslan.memento.repository.UserRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientRelationshipService {

    private final PatientRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    public List<PatientRelationshipDto> getActiveRelationships(User user, boolean excludeDoctors) {
        if (user.getRole() == UserRole.PATIENT) {
            if (excludeDoctors) {
                return relationshipRepository.findByPatient_UserIdAndRelationshipTypeNotAndIsActiveTrue(
                                user.getUserId(), RelationshipType.DOCTOR).stream()
                        .map(MapperUtil::toPatientRelationshipDto).collect(Collectors.toList());
            }
            return relationshipRepository.findByPatient_UserIdAndIsActiveTrue(user.getUserId()).stream()
                    .map(MapperUtil::toPatientRelationshipDto).collect(Collectors.toList());
        }
        // if the user is a relative or doctor
        else {
            return relationshipRepository.findByCaregiver_UserIdAndIsActiveTrue(user.getUserId())
                    .stream()
                    .map(MapperUtil::toPatientRelationshipDto)
                    .collect(Collectors.toList());
        }
    }

    public List<PatientRelationshipDto> getInactiveRelationships(User user) {
        if (user.getRole() == UserRole.PATIENT) {
            return relationshipRepository.findByPatient_UserIdAndIsActiveFalse(user.getUserId()).stream()
                    .map(MapperUtil::toPatientRelationshipDto)
                    .collect(Collectors.toList());
        }
        else {
            return relationshipRepository.findByCaregiver_UserIdAndIsActiveFalse(user.getUserId()).stream()
                    .map(MapperUtil::toPatientRelationshipDto)
                    .collect(Collectors.toList());
        }
    }

    @Transactional
    public PatientRelationshipDto addRelationship(PatientRelationshipDto dto, User initiator) {
        // case 1: patients can only add their relatives
        if(initiator.getRole() == UserRole.PATIENT){
            return addRelativeByPatient(dto, initiator);
        } else if (initiator.getRole() == UserRole.DOCTOR) {
            return addPatientByDoctor(dto, initiator);
        }else {
            throw new IllegalStateException("ONLY_PATIENTS_AND_DOCTORS_CAN_INITIATE_RELATIONSHIPS");
        }
    }

    // case 1: patients can only add a relative
    private PatientRelationshipDto addRelativeByPatient(PatientRelationshipDto dto, User patient) {
        if (dto.getTargetEmail() == null) throw new IllegalArgumentException("TARGET_CAREGIVER_EMAIL_REQUIRED");

        User caregiver = userRepository.findByEmail(dto.getTargetEmail())
                .orElseThrow(() -> new EntityNotFoundException("TARGET_CAREGIVER_NOT_FOUND"));

        if (caregiver.getRole() == UserRole.DOCTOR || dto.getRelationshipType() == RelationshipType.DOCTOR) {
            throw new IllegalArgumentException("PATIENTS_CANNOT_ADD_DOCTORS");
        }
        Boolean isPrimary = dto.getIsPrimaryContact() != null ? dto.getIsPrimaryContact() : false;

        return createRelationship(patient, caregiver, dto.getRelationshipType(), isPrimary);
    }

    // case 2: doctors adding patients
    private PatientRelationshipDto addPatientByDoctor(PatientRelationshipDto dto, User doctor) {
        if (dto.getTargetEmail() == null) throw new IllegalArgumentException("TARGET_PATIENT_EMAIL_REQUIRED");

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
    private PatientRelationshipDto createRelationship(User patient, User caregiver, RelationshipType type, Boolean isPrimaryContact) {

        if (patient.getUserId().equals(caregiver.getUserId())) {
            throw new IllegalArgumentException("SELF_RELATION_NOT_ALLOWED");
        }
        // duplicate check
        Optional<PatientRelationship> existingRel = relationshipRepository
                .findByPatient_UserIdAndCaregiver_UserId(patient.getUserId(), caregiver.getUserId());

        if (existingRel.isPresent()) {
            if (Boolean.TRUE.equals(existingRel.get().getIsActive())) {
                throw new IllegalStateException("RELATIONSHIP_ALREADY_EXISTS_AND_ACTIVE");
            } else {
                throw new IllegalStateException("RELATIONSHIP_EXISTS_BUT_INACTIVE_PLEASE_REACTIVATE");
            }
        }

        PatientRelationship relationship = PatientRelationship.builder()
                .patient(patient)
                .caregiver(caregiver)
                .relationshipType(type)
                .isPrimaryContact(isPrimaryContact != null ? isPrimaryContact : false)
                .isActive(true)
                .build();

        return MapperUtil.toPatientRelationshipDto(relationshipRepository.save(relationship));
    }

    @Transactional
    public PatientRelationshipDto updateRelationship(Integer relationshipId, PatientRelationshipDto dto, User initiator) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

        boolean isPatient = relationship.getPatient().getUserId().equals(initiator.getUserId());
        boolean isCaregiver = relationship.getCaregiver().getUserId().equals(initiator.getUserId());

        if (!isPatient && !isCaregiver) {
            throw new AccessDeniedException("YOU_ARE_NOT_PART_OF_THIS_RELATIONSHIP");
        }

        // if the relationship is currently doctor-patient, only the doctor can edit
        if (relationship.getRelationshipType() == RelationshipType.DOCTOR && !isCaregiver) {
            throw new IllegalStateException("ONLY_DOCTORS_CAN_UPDATE_DOCTOR_RELATIONSHIPS");
        }

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

        return MapperUtil.toPatientRelationshipDto(relationshipRepository.save(relationship));
    }

    // toggle to deactivate or reactivate relationships
    @Transactional
    public PatientRelationshipDto toggleActivation(Integer relationshipId, User initiator) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

        boolean isPatient = relationship.getPatient().getUserId().equals(initiator.getUserId());
        boolean isCaregiver = relationship.getCaregiver().getUserId().equals(initiator.getUserId());
        if (!isPatient && !isCaregiver) {
            throw new AccessDeniedException("YOU_ARE_NOT_PART_OF_THIS_RELATIONSHIP");
        }

        relationship.setIsActive(!relationship.getIsActive());
        return MapperUtil.toPatientRelationshipDto(relationshipRepository.save(relationship));
    }

    // toggle to change primary contacts
    @Transactional
    public PatientRelationshipDto togglePrimaryContactStatus(Integer relationshipId) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

        // null check, if bool is null, it becomes false
        boolean currentStatus = Boolean.TRUE.equals(relationship.getIsPrimaryContact());
        relationship.setIsPrimaryContact(!currentStatus); // reversing primary contact status

        return MapperUtil.toPatientRelationshipDto(relationshipRepository.save(relationship));
    }
}
