package com.emiraslan.memento.entity;

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
@Table(name = "MedicationSchedules")
public class MedicationSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Integer scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patient;

    // If the doctor gets removed from the database, this field will become null to protect the patient's medical history.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_user_id", nullable = true)
    private User doctor;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @Column(name = "dosage", length = 100)
    private String dosage;

    @Column(name = "notes", columnDefinition = "NVARCHAR(MAX)")
    private String notes;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    // PRN: Pro re nata(as needed). If the medicine is to be taken on need, isPrn = 1. If it is timed, isPrn = 0.
    @Column(name = "is_prn")
    @Builder.Default
    private Boolean isPrn = false; // By default, all medicines are to be taken in a timely manner.

    // Acts as a protection against false diagnosis without deleting/editing the entire prescription.
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true; // the patient wont be notified for medicines that are isActive = false
}