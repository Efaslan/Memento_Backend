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
public class PatientCardDto { // patient view cards for doctor web application
    private Integer relationshipId;
    private Integer patientId;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDate dateOfBirth;
    private Integer heightCm;
    private Double weightKg;
    private BloodType bloodType;
    private String emergencyNotes;
}
