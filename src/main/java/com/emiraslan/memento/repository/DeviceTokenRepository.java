package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

    // sees if a token already exists to not create duplicates
    Optional<DeviceToken> findByFcmToken(String fcmToken);

    void deleteByFcmToken(String fcmToken);
}
