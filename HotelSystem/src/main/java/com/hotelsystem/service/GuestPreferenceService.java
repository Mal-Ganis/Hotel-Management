package com.hotelsystem.service;

import com.hotelsystem.dto.GuestPreferenceDto;
import com.hotelsystem.entity.Guest;
import com.hotelsystem.entity.GuestPreference;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.repository.GuestPreferenceRepository;
import com.hotelsystem.repository.GuestRepository;
import com.hotelsystem.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GuestPreferenceService {

    private final GuestPreferenceRepository preferenceRepository;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;

    public List<GuestPreferenceDto> getPreferencesByGuestId(Long guestId) {
        return preferenceRepository.findByGuestIdOrderByFrequencyDescLastUsedAtDesc(guestId).stream()
                .map(GuestPreferenceDto::fromEntity)
                .collect(Collectors.toList());
    }

    public GuestPreferenceDto createPreference(Long guestId, GuestPreferenceDto dto) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("宾客不存在"));

        GuestPreference preference = dto.toEntity();
        preference.setGuest(guest);
        
        GuestPreference saved = preferenceRepository.save(preference);
        return GuestPreferenceDto.fromEntity(saved);
    }

    public GuestPreferenceDto updatePreference(Long id, GuestPreferenceDto dto) {
        GuestPreference existing = preferenceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("偏好记录不存在"));

        existing.setPreferenceType(dto.getPreferenceType());
        existing.setPreferenceValue(dto.getPreferenceValue());
        existing.setDescription(dto.getDescription());
        if (dto.getFrequency() != null) {
            existing.setFrequency(dto.getFrequency());
        }

        GuestPreference updated = preferenceRepository.save(existing);
        return GuestPreferenceDto.fromEntity(updated);
    }

    public void deletePreference(Long id) {
        preferenceRepository.deleteById(id);
    }

    /**
     * 从历史订单中自动提取宾客偏好
     */
    @Transactional
    public void extractPreferencesFromHistory(Long guestId) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new RuntimeException("宾客不存在"));

        List<Reservation> reservations = reservationRepository.findByGuestId(guestId);

        // 统计房型偏好
        Map<String, Long> roomTypeCounts = reservations.stream()
                .filter(r -> r.getRoom() != null && r.getRoom().getRoomType() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getRoom().getRoomType(),
                        Collectors.counting()
                ));

        roomTypeCounts.forEach((roomType, count) -> {
            GuestPreference existing = preferenceRepository.findByGuestId(guestId).stream()
                    .filter(p -> "房型偏好".equals(p.getPreferenceType()) && roomType.equals(p.getPreferenceValue()))
                    .findFirst()
                    .orElse(null);

            if (existing != null) {
                existing.setFrequency(existing.getFrequency() + count.intValue());
                existing.setLastUsedAt(LocalDateTime.now());
                preferenceRepository.save(existing);
            } else {
                GuestPreference preference = new GuestPreference();
                preference.setGuest(guest);
                preference.setPreferenceType("房型偏好");
                preference.setPreferenceValue(roomType);
                preference.setDescription("从历史订单中自动提取");
                preference.setFrequency(count.intValue());
                preference.setLastUsedAt(LocalDateTime.now());
                preferenceRepository.save(preference);
            }
        });
    }

    /**
     * 获取推荐房型（基于宾客偏好）
     */
    public List<String> getRecommendedRoomTypes(Long guestId) {
        return preferenceRepository.findByGuestIdOrderByFrequencyDescLastUsedAtDesc(guestId).stream()
                .filter(p -> "房型偏好".equals(p.getPreferenceType()))
                .map(GuestPreference::getPreferenceValue)
                .collect(Collectors.toList());
    }
}

