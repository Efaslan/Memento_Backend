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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientRelationshipService {

    private final PatientRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    public List<PatientRelationshipDto> getActiveRelationships(Integer patientId, boolean excludeDoctors) {
        List<PatientRelationship> relationships;

        if (excludeDoctors) {
            // filter doctor relations out
            relationships = relationshipRepository.findByPatient_UserIdAndRelationshipTypeNotAndIsActiveTrue(
                    patientId,
                    RelationshipType.DOCTOR
            );
        } else {
            // brings all relationships
            relationships = relationshipRepository.findByPatient_UserIdAndIsActiveTrue(patientId);
        }

        return relationships.stream()
                .map(MapperUtil::toPatientRelationshipDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PatientRelationshipDto addRelationship(PatientRelationshipDto dto) {

        User patient = userRepository.findById(dto.getPatientUserId())
                .orElseThrow(() -> new EntityNotFoundException("USER_PATIENT_NOT_FOUND"));

        // find caregiver through email
        if (dto.getCaregiverEmail() == null || dto.getCaregiverEmail().isEmpty()) {
            throw new IllegalArgumentException("EMAIL_IS_REQUIRED");
        }
        User caregiver = userRepository.findByEmail(dto.getCaregiverEmail())
                .orElseThrow(() -> new EntityNotFoundException("EMAIL_NOT_REGISTERED"));

        // checks:
        if (patient.getUserId().equals(caregiver.getUserId())) {
            throw new IllegalArgumentException("SELF_RELATION_NOT_ALLOWED");
        }
        if (dto.getRelationshipType() == RelationshipType.DOCTOR && caregiver.getRole() != UserRole.DOCTOR) {
            throw new IllegalArgumentException("CAREGIVER_NOT_DOCTOR");
        }

        // checking if that relation exist to prevent unique constraint error
        Optional<PatientRelationship> existingRel = relationshipRepository
                .findByPatient_UserId(patient.getUserId()) // bring all relations, active or not
                .stream()
                .filter(r -> r.getCaregiver().getUserId().equals(caregiver.getUserId()))
                .findFirst();

        PatientRelationship relationship;

        if (existingRel.isPresent()) {
            // if it exists, update and activate the relationship
            relationship = existingRel.get();
            relationship.setIsActive(true);
            relationship.setRelationshipType(dto.getRelationshipType());
            relationship.setIsPrimaryContact(dto.getIsPrimaryContact() != null ? dto.getIsPrimaryContact() : false);
        } else {
            // if not, add a new relationship
            relationship = MapperUtil.toPatientRelationshipEntity(dto, patient, caregiver);
            relationship.setIsActive(true);
        }

        return MapperUtil.toPatientRelationshipDto(relationshipRepository.save(relationship));
    }

    // deactivate instead of deleting relationships to keep track of past doctors
    @Transactional
    public void deactivateRelationship(Integer relationshipId) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

        relationship.setIsActive(false);
        relationshipRepository.save(relationship);
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
