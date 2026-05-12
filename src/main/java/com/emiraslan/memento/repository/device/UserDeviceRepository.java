package com.emiraslan.memento.repository.device;

import com.emiraslan.memento.entity.UserDevice;
import com.emiraslan.memento.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDeviceRepository extends JpaRepository<UserDevice, Integer> {

    @Query("SELECT d FROM UserDevice d JOIN FETCH d.user WHERE d.deviceId = :deviceId")
    Optional<UserDevice> findByIdWithUser(@Param("deviceId") Integer deviceId);

    Optional<UserDevice> findByDeviceIdAndUser(Integer deviceId, User user);

    List<UserDevice> findAllByUser_UserId(Integer userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserDevice d SET d.biometricEnabled = :isEnabled WHERE d.deviceId = :deviceId")
    int updateBiometricStatus(@Param("deviceId") Integer deviceId, @Param("isEnabled") boolean isEnabled);
}