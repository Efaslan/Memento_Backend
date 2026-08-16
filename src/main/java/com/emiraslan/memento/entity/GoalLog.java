package com.emiraslan.memento.entity;

import com.emiraslan.memento.enums.GoalStatus;
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
@Table(name = "goal_logs", indexes = {
        // for finding today's logs
        @Index(name = "idx_goallog_goal_date", columnList = "goal_id, created_at")
})
public class GoalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "goal_log_id")
    private Integer goalLogId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private GoalStatus status;

    // if the user couldn't full complete the goal, for example, 3000 steps of the 5000 steps goal
    @Column(name = "progress_value")
    private Double progressValue;

    @Column(name = "log_target_value")
    private Double logTargetValue;

    @Column(name = "log_unit", length = 20)
    private String logUnit;

    // Time isn't needed because this is daily
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDate createdAt = LocalDate.now();
}
