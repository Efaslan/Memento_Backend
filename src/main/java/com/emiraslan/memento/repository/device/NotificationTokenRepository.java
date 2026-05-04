package com.emiraslan.memento.repository.device;

import com.emiraslan.memento.entity.NotificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationTokenRepository extends JpaRepository<NotificationToken, Integer> {

    // sees if a token already exists to not create duplicates
    Optional<NotificationToken> findByFcmToken(String fcmToken);

    Optional<NotificationToken> findByUserDevice_DeviceId(Integer deviceId);

    @Query("SELECT n FROM NotificationToken n JOIN FETCH n.userDevice d JOIN FETCH d.user")
    List<NotificationToken> findAllWithDeviceAndUser();
}
