package com.emiraslan.memento.repository.user;

import com.emiraslan.memento.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Finding a user by email for login
    Optional<User> findByEmail(String email);

    // Checking email uniqueness
    boolean existsByEmail(String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM User u WHERE u.isEmailVerified = false AND u.createdAt < :cutoffTime")
    int deleteUnverifiedUsersOlderThan(@Param("cutoffTime") LocalDateTime cutoffTime);
}