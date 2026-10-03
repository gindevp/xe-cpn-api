package com.mycompany.myapp.repository;

import java.math.BigDecimal;
import java.time.Instant;

public record ReceiptListRow(
    Long id,
    String receiptCode,
    String payerName,
    String payerCode,
    BigDecimal totalAmount,
    Instant createdAt,
    String createdByUsername,
    String officeCode,
    Instant confirmedAt,
    String confirmedByUsername,
    Boolean hasConfirmProof,
    Boolean hasTransferProof
) {}
