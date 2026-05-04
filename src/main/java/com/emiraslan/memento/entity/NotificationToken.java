package com.emiraslan.memento.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_tokens", indexes = {
        @Index(name = "idx_notification_token_device_id", columnList = "device_id")
})
public class NotificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Integer tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private UserDevice userDevice;

    // nullable in case the user doesn't allow notifications
    @Column(name = "fcm_token", unique = true) // indexed with unique
    private String fcmToken;

    @Column(name = "last_updated")
    @Builder.Default
    private Instant lastUpdated = Instant.now();
}