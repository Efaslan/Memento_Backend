package com.emiraslan.memento.repository.user;

import com.emiraslan.memento.entity.user.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Integer> {

}
