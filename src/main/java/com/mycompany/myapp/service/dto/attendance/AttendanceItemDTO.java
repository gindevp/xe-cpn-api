package com.mycompany.myapp.service.dto.attendance;

import java.time.Instant;

public record AttendanceItemDTO(Long id, Instant checkedAt, String officeCode, String officeName) {}
