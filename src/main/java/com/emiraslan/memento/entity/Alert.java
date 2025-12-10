package com.emiraslan.memento.entity;

import com.emiraslan.memento.enums.AlertStatus;
import com.emiraslan.memento.enums.AlertType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Integer alertId;

    @ManyToOne(fetch = FetchType.LAZY) // corresponding user's whole data isn't automatically pulled when we fetch Alerts, improves performance
    @JoinColumn(name = "patient_user_id", nullable = false) // Foreign Key
    private User patient;

    // shows which caregiver is dealing with the alert
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by_user_id")
    private User acknowledgedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 50)
    private AlertType alertType;

    @Column(name = "alert_timestamp", columnDefinition = "DATETIME2 DEFAULT GETDATE()")
    @Builder.Default
    private LocalDateTime alertTimestamp = LocalDateTime.now();

    // can be null in case the user's location is off in their phone
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    // PENDING -> CANCELLED/SENT -> ACKNOWLEDGED
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    @Builder.Default
    private AlertStatus status = AlertStatus.PENDING; // Default value is "PENDING" whenever an Alert is created

    @Column(name = "details", columnDefinition = "NVARCHAR(MAX)")
    private String details;
}