package com.emiraslan.memento.entity;

import com.emiraslan.memento.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PatientRelationships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"patient_user_id", "caregiver_user_id"})
})
public class PatientRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "relationship_id")
    private Integer relationshipId;

    // Relationship's Patient side
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false) // FK
    private User patient;

    // Relationship's Caregiver(Doctor/Relative) side
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caregiver_user_id", nullable = false) // FK
    private User caregiver;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", length = 50)
    private RelationshipType relationshipType;

    @Column(name = "is_primary_contact")
    @Builder.Default
    private Boolean isPrimaryContact = false; // By default, the other side will not be contacted during Alerts

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // By default, the relationship is active
}