package com.emiraslan.memento.entity;

import com.emiraslan.memento.enums.DeviceType;
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
@Table(name = "DeviceTokens")
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Integer tokenId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 50)
    private DeviceType deviceType; // only ANDROID for now

    @Column(name = "last_updated", columnDefinition = "DATETIME2 DEFAULT GETDATE()")
    @Builder.Default
    private LocalDateTime lastUpdated = LocalDateTime.now();
}