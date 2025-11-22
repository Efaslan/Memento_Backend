package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    // Finding a user by email for login
    Optional<User> findByEmail(String email);

    // Checking email uniqueness
    boolean existsByEmail(String email);

    // JPA automatically includes CRUD, and findById, deleteById, and existsById functions.
}