package com.emiraslan.memento.repository.device;

import com.emiraslan.memento.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    // find the user's device through its unique refreshToken UUID we generated
    @Query("SELECT r FROM RefreshToken r JOIN FETCH r.userDevice d JOIN FETCH d.user WHERE r.refreshToken = :token")
    Optional<RefreshToken> findByRefreshToken(@Param("token") String refreshToken);
}
