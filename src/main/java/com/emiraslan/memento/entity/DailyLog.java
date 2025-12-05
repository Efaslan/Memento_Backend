package com.emiraslan.memento.entity;

import com.emiraslan.memento.enums.DailyLogType;
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
@Table(name = "DailyLogs")
public class DailyLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_log_id")
    private Integer dailyLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false) // FK
    private User patient;

    @Enumerated(EnumType.STRING) // FOOD or WATER
    @Column(name = "log_type", nullable = false, length = 50)
    private DailyLogType dailyLogType;

    // For food
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    // For water
    @Column(name = "quantity_ml")
    private Integer quantityMl;

    @Column(name = "created_at", columnDefinition = "DATETIME2 DEFAULT GETDATE()")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
