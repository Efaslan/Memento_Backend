package com.emiraslan.memento.entity;

import com.emiraslan.memento.entity.user.User;
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
@Table(name = "user_devices", indexes = {
        @Index(name = "idx_user_device_user_id", columnList = "user_id")
})
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_id")
    private Integer deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // "Samsung Galaxy S23"
    @Column(name = "device_model", length = 100)
    private String deviceModel;

    // "Android 14"
    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "public_key", length = 1000)
    private String publicKey;

    @Column(name = "biometric_enabled")
    @Builder.Default
    private Boolean biometricEnabled = false;

    @Column(name = "last_active")
    @Builder.Default
    private LocalDateTime lastActive = LocalDateTime.now();

    // --- CASCADE DELETE ---
    // if a device is deleted, all tokens related to it are deleted as well
    @OneToOne(mappedBy = "userDevice", cascade = CascadeType.ALL, orphanRemoval = true)
    private RefreshToken refreshToken;

    @OneToOne(mappedBy = "userDevice", cascade = CascadeType.ALL, orphanRemoval = true)
    private NotificationToken notificationToken;
}