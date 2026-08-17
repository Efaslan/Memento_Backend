package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.response.BasicStringResponse;
import com.emiraslan.memento.dto.response.RelationshipResponseDto;
import com.emiraslan.memento.dto.request.RelationshipRequestDto;
import com.emiraslan.memento.entity.user.PatientRelationship;
import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.RelationshipType;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.user.PatientRelationshipRepository;
import com.emiraslan.memento.repository.user.UserRepository;
import com.emiraslan.memento.service.notification.OtpService;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PatientRelationshipService {

    private final PatientRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final OtpService otpService;
    private final StringRedisTemplate redisTemplate;

    private String buildRedisKey(Integer patientId, Integer caregiverId) {
        return "relationship:" + patientId + ":" + caregiverId;
    }

    public List<RelationshipResponseDto> getActiveRelationships(User user) {
        return relationshipRepository.findAllActiveRelationshipsByUserId(user.getUserId())
                .stream()
                .map(MapperUtil::toRelationshipResponseDto)
                .toList();
    }

    @Transactional
    public BasicStringResponse relationshipRequestByPatient(String email, User initiator){
        otpService.generateAndSendOtpForRelationshipInvitation(email, initiator);

        return new BasicStringResponse("6-digit OTP successfully sent to the target email.");
    }

    @Transactional
    public RelationshipResponseDto addRelationship(RelationshipRequestDto dto, User initiatorPatient) {
        User caregiver = userRepository.findByEmail(dto.getTargetEmail())
                .orElseThrow(() -> new EntityNotFoundException("TARGET_CAREGIVER_NOT_FOUND"));

        // checking the relationship role first to prevent OTP from being deleted early
        if (caregiver.getRole() == UserRole.PATIENT) {
            throw new IllegalArgumentException("PATIENTS_CAN_ONLY_ADD_RELATIVES");
        }

        // OTP is needed for when patients add relationships
        if (dto.getOtpCode() == null || dto.getOtpCode().trim().isEmpty()) {
            throw new IllegalArgumentException("OTP_CODE_IS_REQUIRED");
        }
        otpService.validateOtpForRelationshipInvitation(dto.getTargetEmail(), initiatorPatient, dto.getOtpCode());

        Boolean isPrimary = dto.getIsPrimaryContact() != null ? dto.getIsPrimaryContact() : false;

        return createRelationship(initiatorPatient, caregiver, dto.getRelationshipType(), isPrimary);
    }

    // common logic for saving/updating relationships
    private RelationshipResponseDto createRelationship(User patient, User caregiver, RelationshipType type, Boolean isPrimaryContact) {

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
        PatientRelationship savedRelationship = relationshipRepository.save(relationship);

        // Adding relationships to Redis for quick lookups during Relative relationship security checks
        String rediskey = buildRedisKey(patient.getUserId(), caregiver.getUserId());
        redisTemplate.opsForValue().set(rediskey, "true"); // value doesn't matter

        return MapperUtil.toRelationshipResponseDto(savedRelationship);
    }

    @Transactional
    public RelationshipResponseDto updateRelationship(Integer relationshipId, RelationshipResponseDto dto) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

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

    @Transactional
    public void deactivateRelationship(Integer relationshipId) {
        PatientRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));
        relationship.setIsActive(false);
        relationshipRepository.save(relationship);

        // delete from Redis as well
        String redisKey = buildRedisKey(relationship.getPatient().getUserId(), relationship.getCaregiver().getUserId());
        redisTemplate.delete(redisKey);
    }
}
