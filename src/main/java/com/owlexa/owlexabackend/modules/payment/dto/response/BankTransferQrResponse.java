package com.owlexa.owlexabackend.modules.payment.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BankTransferQrResponse {

    private Long paymentId;
    private String paymentCode;
    private BigDecimal amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String transferContent;
    private String qrContent;
    private String qrImage;
    private Instant expiresAt;
    private String status;
}
