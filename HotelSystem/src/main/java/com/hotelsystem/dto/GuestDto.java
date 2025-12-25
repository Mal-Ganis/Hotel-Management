package com.hotelsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hotelsystem.entity.Guest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class GuestDto {
    private Long id;

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过50个字符")
    private String fullName;

    @NotBlank(message = "身份证号不能为空")
    @Size(max = 20, message = "身份证号长度不能超过20个字符")
    private String idCardNumber;

    @Size(max = 20, message = "手机号长度不能超过20个字符")
    private String phone;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Size(min = 6, message = "密码长度至少6个字符")
    private String password;

    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;

    private Guest.Gender gender;
    private LocalDate dateOfBirth;

    @Size(max = 200, message = "地址长度不能超过200个字符")
    private String address;

    @Size(max = 100, message = "偏好长度不能超过100个字符")
    private String preferences;

    @Size(max = 500, message = "特殊要求长度不能超过500个字符")
    private String specialRequests;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static GuestDto fromEntity(Guest guest) {
        GuestDto dto = new GuestDto();
        dto.setId(guest.getId());
        dto.setFullName(guest.getFullName());
        dto.setIdCardNumber(guest.getIdCardNumber());
        dto.setPhone(guest.getPhone());
        dto.setEmail(guest.getEmail());
        dto.setGender(guest.getGender());
        dto.setDateOfBirth(guest.getDateOfBirth());
        dto.setAddress(guest.getAddress());
        dto.setPreferences(guest.getPreferences());
        dto.setSpecialRequests(guest.getSpecialRequests());
        dto.setCreatedAt(guest.getCreatedAt());
        dto.setUpdatedAt(guest.getUpdatedAt());
        return dto;
    }

    public Guest toEntity() {
        Guest guest = new Guest();
        guest.setFullName(this.fullName);
        guest.setIdCardNumber(this.idCardNumber);
        guest.setPhone(this.phone);
        guest.setEmail(this.email);
        guest.setPassword(this.password);
        guest.setGender(this.gender);
        guest.setDateOfBirth(this.dateOfBirth);
        guest.setAddress(this.address);
        guest.setPreferences(this.preferences);
        guest.setSpecialRequests(this.specialRequests);
        return guest;
    }
}
