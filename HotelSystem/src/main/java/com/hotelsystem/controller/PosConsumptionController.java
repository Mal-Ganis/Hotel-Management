package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.PosConsumptionDto;
import com.hotelsystem.service.PosConsumptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/pos")
@RequiredArgsConstructor
public class PosConsumptionController {

    private final PosConsumptionService posConsumptionService;

    @GetMapping("/reservation/{reservationId}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<PosConsumptionDto>>> getConsumptionsByReservation(
            @PathVariable Long reservationId) {
        List<PosConsumptionDto> consumptions = posConsumptionService.getConsumptionsByReservationId(reservationId);
        return ResponseEntity.ok(ApiResponse.success(consumptions));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PosConsumptionDto>> createConsumption(
            @RequestBody PosConsumptionDto dto,
            Authentication authentication) {
        try {
            String createdBy = authentication != null ? authentication.getName() : "system";
            PosConsumptionDto created = posConsumptionService.createConsumption(dto, createdBy);
            return ResponseEntity.ok(ApiResponse.success("创建成功", created));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<PosConsumptionDto>> updateConsumption(
            @PathVariable Long id,
            @RequestBody PosConsumptionDto dto) {
        try {
            PosConsumptionDto updated = posConsumptionService.updateConsumption(id, dto);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteConsumption(@PathVariable Long id) {
        try {
            posConsumptionService.deleteConsumption(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}

