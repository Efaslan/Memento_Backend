package com.emiraslan.memento.entity;

import com.emiraslan.memento.entity.user.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "last_active")
    @Builder.Default
    private Instant lastActive = Instant.now();

    // --- CASCADE DELETE ---
    // if a device is deleted, all tokens related to it are deleted as well
    @OneToMany(mappedBy = "userDevice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "userDevice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<NotificationToken> notificationTokens = new ArrayList<>();
}