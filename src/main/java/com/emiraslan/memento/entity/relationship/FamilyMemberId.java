package com.emiraslan.memento.entity.relationship;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyMemberId implements Serializable {
    // storing here because of composite PKS

    private Integer familyId;
    private Integer userId;
}