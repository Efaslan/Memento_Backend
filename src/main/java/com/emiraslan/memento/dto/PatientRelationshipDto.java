package com.emiraslan.memento.dto;

import com.emiraslan.memento.enums.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRelationshipDto {
    private Integer relationshipId;
    private Integer patientUserId;
    private Integer caregiverUserId;
    // For displaying additional info about the caregiver
    private String caregiverName;
    private String caregiverPhone;
    private String caregiverEmail;

    // used when initiating relationships
    private String targetEmail;

    private RelationshipType relationshipType;
    private Boolean isPrimaryContact;
    private Boolean isActive;
}