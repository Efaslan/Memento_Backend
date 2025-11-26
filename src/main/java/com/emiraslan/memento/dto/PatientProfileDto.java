package com.emiraslan.memento.dto;

import com.emiraslan.memento.enums.BloodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientProfileDto {
    private Integer patientUserId;
    // additional info from User
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    // from PatientProfile
    private LocalDate dateOfBirth;
    private Integer heightCm;
    private Double weightKg;
    private BloodType bloodType;
    private String emergencyNotes;
}