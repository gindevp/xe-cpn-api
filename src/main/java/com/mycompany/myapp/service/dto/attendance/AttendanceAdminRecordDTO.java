package com.mycompany.myapp.service.dto.attendance;

import java.time.Instant;

public record AttendanceAdminRecordDTO(Long id, String login, String officeCode, String officeName, Instant checkedAt, String clientIp) {}
