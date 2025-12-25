package com.hotelsystem.repository;

import com.hotelsystem.entity.GuestPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GuestPreferenceRepository extends JpaRepository<GuestPreference, Long> {
    List<GuestPreference> findByGuestId(Long guestId);
    List<GuestPreference> findByGuestIdOrderByFrequencyDescLastUsedAtDesc(Long guestId);
    List<GuestPreference> findByPreferenceType(String preferenceType);
}

