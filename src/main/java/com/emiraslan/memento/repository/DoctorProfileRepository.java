package com.emiraslan.memento.repository;

import com.emiraslan.memento.entity.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Integer> {
    // Since the userId is the same, there isn't a need for extra methods.
}