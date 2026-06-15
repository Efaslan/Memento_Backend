package com.emiraslan.memento.entity;

import com.emiraslan.memento.entity.user.User;
import com.emiraslan.memento.enums.GoalType;
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
@Table(name = "goals", indexes = {
        // for showcasing daily goals in patient's homescreen
        @Index(name = "idx_goal_patient_active", columnList = "patient_user_id, is_active")
})
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_id")
    private Integer goalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_user_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_user_id")
    private User creator;

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 20)
    private GoalType goalType;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    // 2.5 liter or 5000 steps
    @Column(name = "target_value")
    private Double targetValue;

    // Liter, steps, glasses, minutes
    @Column(name = "unit", length = 20)
    private String unit;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}