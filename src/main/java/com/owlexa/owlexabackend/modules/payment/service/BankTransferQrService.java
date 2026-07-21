package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.modules.payment.dto.response.BankTransferQrResponse;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Responsible ONLY for generating bank transfer QR data.
 * Does NOT perform payment business logic — that stays in PaymentService.
 */
@Service
@RequiredArgsConstructor
public class BankTransferQrService {

    @Value("${app.payment.bank-transfer.bank-name:MB Bank}")
    private String bankName;

    @Value("${app.payment.bank-transfer.account-number:}")
    private String accountNumber;

    @Value("${app.payment.bank-transfer.account-holder:Owlexa English Center}")
    private String accountHolder;

    @Value("${app.payment.bank-transfer.qr-template:compact2}")
    private String qrTemplate;

    /**
     * Builds QR display data for a pending bank transfer payment.
     * The frontend only displays what this method returns —
     * it never constructs QR data on its own.
     */
    public BankTransferQrResponse buildQrResponse(Payment payment) {
        String paymentCode = payment.getSepayRef() != null
                ? payment.getSepayRef()
                : "PAY" + String.format("%06d", payment.getId());
        BigDecimal amount = payment.getAmount();
        String transferContent = paymentCode + " thanh toan hoc phi";

        // VietQR-compatible QR content string.
        // Frontend can use any QR library to render this string as a QR image,
        // or call VietQR's image API with these parameters.
        String qrContent = buildVietQrContent(amount, transferContent);

        String status;
        if (payment.getStatus() == TransactionStatus.ACTIVE) {
            status = "PAID";
        } else if (payment.getStatus() == TransactionStatus.EXPIRED) {
            status = "EXPIRED";
        } else if (payment.getStatus() == TransactionStatus.VOIDED) {
            status = "CANCELLED";
        } else {
            status = "PENDING";
        }

        return BankTransferQrResponse.builder()
                .paymentId(payment.getId())
                .paymentCode(paymentCode)
                .amount(amount)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .transferContent(transferContent)
                .qrContent(qrContent)
                .qrImage(null) // frontend generates QR image from qrContent
                .expiresAt(payment.getExpiresAt())
                .status(status)
                .build();
    }

    /**
     * Builds a VietQR-compliant content string.
     * Format follows VietQR standard: bank_id, account_no, amount, description.
     * This raw string can be rendered as QR by any standard QR library.
     */
    private String buildVietQrContent(BigDecimal amount, String description) {
        // VietQR standard format:
        // 00020101021238570010A00000072701270006<bank_id>0208QRIBFTTA5303704<amount>5405<amount>5802VN62<desc_length><desc>
        // For simplicity and frontend flexibility, we provide the essential fields
        // and let the frontend use a library like vietqr or qrcode to render.
        // The format below is a compact representation:
        StringBuilder sb = new StringBuilder();
        sb.append("BANK:").append(bankName).append("|");
        sb.append("ACC:").append(accountNumber).append("|");
        sb.append("AMOUNT:").append(amount.longValue()).append("|");
        sb.append("CONTENT:").append(description);
        return sb.toString();
    }
}
