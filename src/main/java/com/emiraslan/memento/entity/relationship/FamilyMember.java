package com.emiraslan.memento.entity.relationship;

import com.emiraslan.memento.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "FamilyMembers")
public class FamilyMember {

    @EmbeddedId
    private FamilyMemberId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("familyId")
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}