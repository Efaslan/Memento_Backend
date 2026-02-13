package com.emiraslan.memento.repository.relationship;

import com.emiraslan.memento.entity.relationship.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Integer> {
}