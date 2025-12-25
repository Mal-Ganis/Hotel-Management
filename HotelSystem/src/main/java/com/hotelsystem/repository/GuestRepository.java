package com.hotelsystem.repository;

import com.hotelsystem.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByIdCardNumber(String idCardNumber);
    List<Guest> findByFullNameContainingIgnoreCase(String fullName);
    List<Guest> findByPhone(String phone);
    Boolean existsByIdCardNumber(String idCardNumber);

    Optional<Guest> findByEmail(String email);
}
