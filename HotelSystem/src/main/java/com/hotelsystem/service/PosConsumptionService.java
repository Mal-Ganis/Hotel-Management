package com.hotelsystem.service;

import com.hotelsystem.dto.PosConsumptionDto;
import com.hotelsystem.entity.PosConsumption;
import com.hotelsystem.entity.Reservation;
import com.hotelsystem.repository.PosConsumptionRepository;
import com.hotelsystem.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PosConsumptionService {

    private final PosConsumptionRepository consumptionRepository;
    private final ReservationRepository reservationRepository;

    public List<PosConsumptionDto> getConsumptionsByReservationId(Long reservationId) {
        return consumptionRepository.findByReservationId(reservationId).stream()
                .map(PosConsumptionDto::fromEntity)
                .collect(Collectors.toList());
    }

    public PosConsumptionDto createConsumption(PosConsumptionDto dto, String createdBy) {
        Reservation reservation = reservationRepository.findById(dto.getReservationId())
                .orElseThrow(() -> new RuntimeException("预订不存在"));

        PosConsumption consumption = dto.toEntity();
        consumption.setReservation(reservation);
        consumption.setCreatedBy(createdBy);
        
        PosConsumption saved = consumptionRepository.save(consumption);
        return PosConsumptionDto.fromEntity(saved);
    }

    public void deleteConsumption(Long id) {
        consumptionRepository.deleteById(id);
    }

    public PosConsumptionDto updateConsumption(Long id, PosConsumptionDto dto) {
        PosConsumption existing = consumptionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("消费记录不存在"));

        existing.setItemName(dto.getItemName());
        existing.setCategory(dto.getCategory());
        existing.setQuantity(dto.getQuantity());
        existing.setUnitPrice(dto.getUnitPrice());
        existing.setTotalAmount(dto.getTotalAmount());
        existing.setDescription(dto.getDescription());
        existing.setConsumptionDate(dto.getConsumptionDate());

        PosConsumption updated = consumptionRepository.save(existing);
        return PosConsumptionDto.fromEntity(updated);
    }
}

