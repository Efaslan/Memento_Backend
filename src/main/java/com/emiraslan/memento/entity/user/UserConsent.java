package com.emiraslan.memento.entity.user;

import com.emiraslan.memento.enums.ConsentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_consents", indexes = {
        @Index(name = "idx_user_consents_user_id", columnList = "user_id")
})
public class UserConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "consent_id")
    private Integer consentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Column(name = "document_version", nullable = false, length = 20)
    private String documentVersion;

    // Accepted or denied information
    @Column(name = "is_accepted", nullable = false)
    private Boolean isAccepted;

    @Column(name = "consented_at", nullable = false)
    private LocalDateTime consentedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    // device information taken from User-Agent header
    @Column(name = "user_agent")
    private String userAgent;
}
