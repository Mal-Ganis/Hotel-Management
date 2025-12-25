package com.hotelsystem.controller;

import com.hotelsystem.dto.ApiResponse;
import com.hotelsystem.dto.GuestDto;
import com.hotelsystem.service.GuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;

import com.hotelsystem.repository.GuestRepository;

import java.util.List;

@RestController
@RequestMapping("/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;
    private final GuestRepository guestRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<List<GuestDto>>> getAllGuests() {
        List<GuestDto> guests = guestService.getAllGuests();
        return ResponseEntity.ok(ApiResponse.success(guests));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<GuestDto>> getGuestById(@PathVariable Long id) {
        return guestService.getGuestById(id)
                .map(guest -> ResponseEntity.ok(ApiResponse.success(guest)))
                .orElse(ResponseEntity.ok(ApiResponse.error("宾客不存在")));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GuestDto>> createGuest(@Valid @RequestBody GuestDto guestDto) {
        try {
            GuestDto createdGuest = guestService.createGuest(guestDto);
            return ResponseEntity.ok(ApiResponse.success("宾客创建成功", createdGuest));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<GuestDto>> updateGuest(
            @PathVariable Long id,
            @Valid @RequestBody GuestDto guestDto,
            Authentication authentication) {
        try {
            // 如果是宾客本人，确保只能修改自己的信息
            if (authentication != null && authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GUEST"))) {
                String email = authentication.getName();
                var guestOpt = guestRepository.findByEmail(email);
                if (guestOpt.isEmpty() || !guestOpt.get().getId().equals(id)) {
                    return ResponseEntity.status(403).body(ApiResponse.error("无权修改此宾客信息"));
                }
            } else {
                // 非宾客只能是有权限的员工，由方法级安全在外部控制
            }

            GuestDto updatedGuest = guestService.updateGuest(id, guestDto);
            return ResponseEntity.ok(ApiResponse.success("宾客更新成功", updatedGuest));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('RECEPTIONIST','MANAGER','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGuest(@PathVariable Long id) {
        try {
            guestService.deleteGuest(id);
            return ResponseEntity.ok(ApiResponse.success("宾客删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    // 获取当前认证宾客信息
    @GetMapping("/me")
    @PreAuthorize("hasRole('GUEST')")
    public ResponseEntity<ApiResponse<GuestDto>> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        return guestRepository.findByEmail(email)
                .map(guest -> ResponseEntity.ok(ApiResponse.success(GuestDto.fromEntity(guest))))
                .orElse(ResponseEntity.ok(ApiResponse.error("未找到当前宾客")));
    }

    @GetMapping("/search/id-card")
    public ResponseEntity<ApiResponse<GuestDto>> getGuestByIdCardNumber(
            @RequestParam String idCardNumber) {
        return guestService.getGuestByIdCardNumber(idCardNumber)
                .map(guest -> ResponseEntity.ok(ApiResponse.success(guest)))
                .orElse(ResponseEntity.ok(ApiResponse.error("未找到对应身份证号的宾客")));
    }

    @GetMapping("/search/name")
    public ResponseEntity<ApiResponse<List<GuestDto>>> searchGuestsByName(
            @RequestParam String name) {
        List<GuestDto> guests = guestService.searchGuestsByName(name);
        return ResponseEntity.ok(ApiResponse.success(guests));
    }

    @GetMapping("/search/phone")
    public ResponseEntity<ApiResponse<List<GuestDto>>> getGuestsByPhone(
            @RequestParam String phone) {
        List<GuestDto> guests = guestService.getGuestsByPhone(phone);
        return ResponseEntity.ok(ApiResponse.success(guests));
    }
}