package com.emiraslan.memento.service;

import com.emiraslan.memento.dto.FamilyDto;
import com.emiraslan.memento.dto.UserDto;
import com.emiraslan.memento.entity.User;
import com.emiraslan.memento.entity.relationship.Family;
import com.emiraslan.memento.entity.relationship.FamilyMemberId;
import com.emiraslan.memento.repository.relationship.FamilyMemberRepository;
import com.emiraslan.memento.repository.relationship.FamilyRepository;
import com.emiraslan.memento.util.MapperUtil;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final RelationshipService relationshipService;

    @Transactional
    public FamilyDto createFamily(String familyName, User creator) {
        Family family = Family.builder()
                .familyName(familyName)
                // createdAt is automated with @Builder.Default
                .build();
        family = familyRepository.save(family); // create the family

        // add the creator as a member to the family
        relationshipService.addMemberToFamily(family, creator);
        return MapperUtil.toFamilyDto(family);
    }

    @Transactional
    public FamilyDto updateFamilyName(Integer familyId, String newName) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new EntityNotFoundException("FAMILY_NOT_FOUND"));

        family.setFamilyName(newName);
        Family savedFamily = familyRepository.save(family);
        return MapperUtil.toFamilyDto(savedFamily);
    }

    public List<UserDto> getFamilyMembers(Integer familyId) {
        if (!familyRepository.existsById(familyId)) {
            throw new EntityNotFoundException("FAMILY_NOT_FOUND");
        }
        return familyMemberRepository.findByFamily_FamilyId(familyId).stream()
                .map(member -> MapperUtil.toUserDto(member.getUser()))
                .collect(Collectors.toList());
    }

    public List<FamilyDto> getUserFamilies(Integer userId) {
        return familyMemberRepository.findByUser_UserId(userId).stream()
                .map(member -> MapperUtil.toFamilyDto(member.getFamily()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeFromFamily(Integer familyId, Integer userId) {
        // find family member with composite PK
        FamilyMemberId memberId = new FamilyMemberId(familyId, userId);

        if (!familyMemberRepository.existsById(memberId)) {
            throw new EntityNotFoundException("MEMBER_NOT_FOUND_IN_FAMILY");
        }

        // remove member from family
        familyMemberRepository.deleteById(memberId);
    }
}
