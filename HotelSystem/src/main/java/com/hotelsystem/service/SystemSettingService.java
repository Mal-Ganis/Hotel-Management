package com.hotelsystem.service;

import com.hotelsystem.dto.SystemSettingDto;
import com.hotelsystem.entity.SystemSetting;
import com.hotelsystem.repository.SystemSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemSettingService {

    private final SystemSettingRepository settingRepository;

    public List<SystemSettingDto> getAllSettings() {
        return settingRepository.findAll().stream()
                .map(SystemSettingDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Optional<SystemSettingDto> getSettingByKey(String key) {
        return settingRepository.findByKey(key)
                .map(SystemSettingDto::fromEntity);
    }

    public SystemSettingDto saveSetting(SystemSettingDto dto) {
        SystemSetting setting = settingRepository.findByKey(dto.getKey())
                .orElse(new SystemSetting());
        
        setting.setKey(dto.getKey());
        setting.setValue(dto.getValue());
        setting.setDescription(dto.getDescription());
        
        SystemSetting saved = settingRepository.save(setting);
        return SystemSettingDto.fromEntity(saved);
    }

    public void deleteSetting(Long id) {
        settingRepository.deleteById(id);
    }
}

