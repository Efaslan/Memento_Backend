package com.emiraslan.memento.repository.user;

import com.emiraslan.memento.entity.user.DoctorProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Integer> {
    // Since the userId is the same, there isn't a need for extra methods.
}