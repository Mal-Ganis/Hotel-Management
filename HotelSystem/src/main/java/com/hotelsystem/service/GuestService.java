package com.hotelsystem.service;

import com.hotelsystem.dto.GuestDto;
import com.hotelsystem.entity.Guest;
import com.hotelsystem.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;
    private final PasswordEncoder passwordEncoder;

    public List<GuestDto> getAllGuests() {
        return guestRepository.findAll().stream()
                .map(GuestDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<GuestDto> getGuestById(Long id) {
        return guestRepository.findById(id)
                .map(GuestDto::fromEntity);
    }

    public GuestDto createGuest(GuestDto guestDto) {
        // 检查身份证号是否已存在
        if (guestRepository.existsByIdCardNumber(guestDto.getIdCardNumber())) {
            throw new RuntimeException("身份证号已存在");
        }

        if (guestDto.getPassword() == null || guestDto.getPassword().length() < 6) {
            throw new RuntimeException("密码长度至少6个字符");
        }

        Guest guest = guestDto.toEntity();
        // 加密密码
        guest.setPassword(passwordEncoder.encode(guest.getPassword()));
        Guest savedGuest = guestRepository.save(guest);
        return GuestDto.fromEntity(savedGuest);
    }

    public GuestDto updateGuest(Long id, GuestDto guestDto) {
        Guest existingGuest = guestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("宾客不存在"));

        // 检查身份证号是否被其他宾客使用
        if (!existingGuest.getIdCardNumber().equals(guestDto.getIdCardNumber()) &&
                guestRepository.existsByIdCardNumber(guestDto.getIdCardNumber())) {
            throw new RuntimeException("身份证号已被其他宾客使用");
        }

        existingGuest.setFullName(guestDto.getFullName());
        existingGuest.setIdCardNumber(guestDto.getIdCardNumber());
        existingGuest.setPhone(guestDto.getPhone());
        existingGuest.setEmail(guestDto.getEmail());
        if (guestDto.getPassword() != null && !guestDto.getPassword().isBlank()) {
            existingGuest.setPassword(passwordEncoder.encode(guestDto.getPassword()));
        }
        existingGuest.setGender(guestDto.getGender());
        existingGuest.setDateOfBirth(guestDto.getDateOfBirth());
        existingGuest.setAddress(guestDto.getAddress());
        existingGuest.setPreferences(guestDto.getPreferences());
        existingGuest.setSpecialRequests(guestDto.getSpecialRequests());

        Guest updatedGuest = guestRepository.save(existingGuest);
        return GuestDto.fromEntity(updatedGuest);
    }

    public void deleteGuest(Long id) {
        if (!guestRepository.existsById(id)) {
            throw new RuntimeException("宾客不存在");
        }
        guestRepository.deleteById(id);
    }

    public Optional<GuestDto> getGuestByIdCardNumber(String idCardNumber) {
        return guestRepository.findByIdCardNumber(idCardNumber)
                .map(GuestDto::fromEntity);
    }

    public List<GuestDto> searchGuestsByName(String name) {
        return guestRepository.findByFullNameContainingIgnoreCase(name).stream()
                .map(GuestDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<GuestDto> getGuestsByPhone(String phone) {
        return guestRepository.findByPhone(phone).stream()
                .map(GuestDto::fromEntity)
                .collect(Collectors.toList());
    }
}