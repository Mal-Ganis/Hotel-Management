package com.hotelsystem.dto;

import com.hotelsystem.entity.SystemSetting;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemSettingDto {
    private Long id;
    private String key;
    private String value;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SystemSettingDto fromEntity(SystemSetting setting) {
        SystemSettingDto dto = new SystemSettingDto();
        dto.setId(setting.getId());
        dto.setKey(setting.getKey());
        dto.setValue(setting.getValue());
        dto.setDescription(setting.getDescription());
        dto.setCreatedAt(setting.getCreatedAt());
        dto.setUpdatedAt(setting.getUpdatedAt());
        return dto;
    }

    public SystemSetting toEntity() {
        SystemSetting setting = new SystemSetting();
        setting.setKey(this.key);
        setting.setValue(this.value);
        setting.setDescription(this.description);
        return setting;
    }
}

