package com.emiraslan.memento.entity;

import com.emiraslan.memento.enums.BloodType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "PatientProfiles")
public class PatientProfile {

    @Id
    @Column(name = "patient_user_id")
    private Integer patientUserId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // gets the patient's id from user relation
    @JoinColumn(name = "patient_user_id") // FK column
    private User patient; // naming the User entity as patient for readability

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "height_cm")
    private Integer heightCm;

    @Column(name = "weight_kg")
    private Double weightKg;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", length = 15)
    private BloodType bloodType;

    @Column(name = "emergency_notes", columnDefinition = "NVARCHAR(MAX)")
    private String emergencyNotes;
}