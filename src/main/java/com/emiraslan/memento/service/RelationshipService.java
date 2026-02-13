package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.RelationshipDto;
import com.emiraslan.memento.dto.RelationshipInitiationDto;
import com.emiraslan.memento.entity.relationship.Family;
import com.emiraslan.memento.entity.relationship.FamilyMember;
import com.emiraslan.memento.entity.relationship.FamilyMemberId;
import com.emiraslan.memento.entity.relationship.Relationship;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.enums.UserRole;
import com.emiraslan.memento.repository.relationship.FamilyMemberRepository;
import com.emiraslan.memento.repository.relationship.FamilyRepository;
import com.emiraslan.memento.repository.relationship.RelationshipRepository;
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
public class RelationshipService {

    private final RelationshipRepository relationshipRepository;
    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;

    public List<RelationshipDto> getRelationships(User user) {
        if (user.getRole() == UserRole.PATIENT) {
            return relationshipRepository.findByPatient_UserId(user.getUserId()).stream()
                    .map(MapperUtil::toPatientRelationshipDto).collect(Collectors.toList());
        }
        // if the user is a relative
        else {
            return relationshipRepository.findByCaregiver_UserId(user.getUserId())
                    .stream()
                    .map(MapperUtil::toPatientRelationshipDto)
                    .collect(Collectors.toList());
        }
    }

    @Transactional
    public RelationshipDto addMemberAndRelationship(Integer familyId, RelationshipInitiationDto dto, User initiator) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new EntityNotFoundException("FAMILY_NOT_FOUND"));

        User relative = userRepository.findByEmail(dto.getRelativeEmail())
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND_WITH_EMAIL: " + dto.getRelativeEmail()));

        if (initiator.getUserId().equals(relative.getUserId())) {
            throw new IllegalArgumentException("SELF_RELATION_NOT_ALLOWED");
        }

        // add relative to family
        if (!familyMemberRepository.existsById_FamilyIdAndId_UserId(familyId, relative.getUserId())) {
            addMemberToFamily(family, relative);
        }

        // check if the relationship already exists
        Optional<Relationship> existingRel = relationshipRepository.findByPatient_UserIdAndCaregiver_UserId(initiator.getUserId(), relative.getUserId());
        if (existingRel.isPresent()) {
            // if there is already a relationship, return it
            return MapperUtil.toPatientRelationshipDto(existingRel.get());
        }

        // add relationship if it does not exist
        Relationship relationship;
            relationship = Relationship.builder()
                    .patient(initiator)
                    .caregiver(relative)
                    .family(family)
                    .relationshipType(dto.getRelationshipType())
                    .isPrimaryContact(Boolean.TRUE.equals(dto.getIsPrimaryContact()))
                    .build();

        return MapperUtil.toPatientRelationshipDto(relationshipRepository.save(relationship));
    }

    @Transactional
    public RelationshipDto updateRelationship(Integer relationshipId, RelationshipDto dto) {
        Relationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));

        // updating relationship type and primary contact status
        if (dto.getRelationshipType() != null) {
            relationship.setRelationshipType(dto.getRelationshipType());
        }
        if (dto.getIsPrimaryContact() != null) {
            relationship.setIsPrimaryContact(dto.getIsPrimaryContact());
        }

        return MapperUtil.toPatientRelationshipDto(relationshipRepository.save(relationship));
    }

    @Transactional
    public void deleteRelationship(Integer relationshipId) {
        Relationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new EntityNotFoundException("RELATIONSHIP_NOT_FOUND"));
        relationshipRepository.delete(relationship);
    }

    // helper
    public void addMemberToFamily(Family family, User user) {
        FamilyMemberId memberId = new FamilyMemberId(family.getFamilyId(), user.getUserId());

        FamilyMember member = FamilyMember.builder()
                .id(memberId)
                .family(family)
                .user(user)
                .build();

        familyMemberRepository.save(member);
    }
}
