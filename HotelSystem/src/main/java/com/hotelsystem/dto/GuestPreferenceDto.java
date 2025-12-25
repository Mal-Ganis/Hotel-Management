package com.hotelsystem.dto;

import com.hotelsystem.entity.GuestPreference;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class GuestPreferenceDto {
    private Long id;
    private Long guestId;
    private String guestName;
    private String preferenceType;
    private String preferenceValue;
    private String description;
    private Integer frequency;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GuestPreferenceDto fromEntity(GuestPreference preference) {
        GuestPreferenceDto dto = new GuestPreferenceDto();
        dto.setId(preference.getId());
        dto.setGuestId(preference.getGuest().getId());
        dto.setGuestName(preference.getGuest().getFullName());
        dto.setPreferenceType(preference.getPreferenceType());
        dto.setPreferenceValue(preference.getPreferenceValue());
        dto.setDescription(preference.getDescription());
        dto.setFrequency(preference.getFrequency());
        dto.setLastUsedAt(preference.getLastUsedAt());
        dto.setCreatedAt(preference.getCreatedAt());
        dto.setUpdatedAt(preference.getUpdatedAt());
        return dto;
    }

    public GuestPreference toEntity() {
        GuestPreference preference = new GuestPreference();
        preference.setPreferenceType(this.preferenceType);
        preference.setPreferenceValue(this.preferenceValue);
        preference.setDescription(this.description);
        preference.setFrequency(this.frequency != null ? this.frequency : 1);
        return preference;
    }
}

