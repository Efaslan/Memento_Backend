package com.emiraslan.memento.repository.relationship;

import com.emiraslan.memento.entity.relationship.FamilyMember;
import com.emiraslan.memento.entity.relationship.FamilyMemberId; // Composite ID
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, FamilyMemberId> {

    // brings all members of the family
    List<FamilyMember> findByFamily_FamilyId(Integer familyId);

    // brings all families of a user
    List<FamilyMember> findByUser_UserId(Integer userId);

    // checks if the user is already registered in the family
    boolean existsById_FamilyIdAndId_UserId(Integer familyId, Integer userId);
}