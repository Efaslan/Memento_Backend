package com.emiraslan.memento.repository.device;

import com.emiraslan.memento.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    // find the user's device through its unique refreshToken UUID we generated
    @Query("SELECT r FROM RefreshToken r JOIN FETCH r.userDevice d JOIN FETCH d.user WHERE r.refreshToken = :token")
    Optional<RefreshToken> findByRefreshToken(@Param("token") String refreshToken);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.userDevice.deviceId = :deviceId")
    void deleteByDeviceId(@Param("deviceId") Integer deviceId);

    // clear automatically removes the deleted entity from JPA cache
    @Modifying(clearAutomatically = true) // @Query annotation's default behaviour is SELECT. With @Modifying, we tell JPA that we will be doing a read or delete action.
    @Query("DELETE FROM RefreshToken r WHERE r.expiryDate < :now")
    int deleteExpiredRefreshTokens(@Param("now") LocalDateTime now);
}
