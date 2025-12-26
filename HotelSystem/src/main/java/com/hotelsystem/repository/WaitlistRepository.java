package com.hotelsystem.repository;

import com.hotelsystem.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    List<Waitlist> findByStatus(Waitlist.WaitlistStatus status);
    List<Waitlist> findByGuestId(Long guestId);
    List<Waitlist> findByDesiredCheckInDateBetweenAndStatus(LocalDate start, LocalDate end, Waitlist.WaitlistStatus status);
}

