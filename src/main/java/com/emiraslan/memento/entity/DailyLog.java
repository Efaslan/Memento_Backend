package com.emiraslan.memento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "daily_logs", indexes = {
        // patientId -> created_at
        @Index(name = "idx_dailylog_patient_date", columnList = "patient_user_id, created_at")
})
public class DailyLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_log_id")
    private Integer dailyLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false) // FK
    private User patient;

    // For food
    @Column(name = "description") // default string is length = 255
    private String description;

    // For water
    @Column(name = "quantity_ml")
    private Integer quantityMl;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
