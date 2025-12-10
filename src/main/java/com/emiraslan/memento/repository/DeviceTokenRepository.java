package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Integer> {

    // brings all devices of a user (phone, tablet etc)
    List<DeviceToken> findByUser_UserId(Integer userId);

    // sees if a token already exists to not create duplicates
    Optional<DeviceToken> findByFcmToken(String fcmToken);

    // deletes a token on logout
    void deleteByFcmToken(String fcmToken);
}
