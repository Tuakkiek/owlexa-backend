package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.modules.payment.dto.response.BankTransferQrResponse;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Generates VietQR-compliant bank transfer QR data.
 * Uses the VietQR Image API (img.vietqr.io) to produce QR images
 * that Vietnamese banking apps can scan directly.
 *
 * Does NOT perform payment business logic — that stays in PaymentService.
 */
@Service
@Slf4j
public class BankTransferQrService {

    @Value("${app.payment.bank-transfer.bank-name:MB Bank}")
    private String bankName;

    @Value("${app.payment.bank-transfer.bank-code:MB}")
    private String bankCode;

    @Value("${app.payment.bank-transfer.account-number:}")
    private String accountNumber;

    @Value("${app.payment.bank-transfer.account-holder:Owlexa English Center}")
    private String accountHolder;

    @Value("${app.payment.bank-transfer.qr-template:compact2}")
    private String qrTemplate;

    /** VietQR image API base URL. */
    private static final String VIETQR_IMAGE_BASE = "https://img.vietqr.io/image";

    /**
     * Builds QR display data for a pending bank transfer payment.
     * Returns both a VietQR image URL (qrImage) and a QR-content string (qrContent)
     * so the frontend can display a banking-app-scannable QR.
     */
    public BankTransferQrResponse buildQrResponse(Payment payment) {
        String paymentCode = payment.getSepayRef() != null
                ? payment.getSepayRef()
                : "PAY" + String.format("%06d", payment.getId());
        BigDecimal amount = payment.getAmount();
        String transferContent = paymentCode + " thanh toan hoc phi";

        // Generate VietQR image API URL — this URL returns a PNG image
        // that Vietnamese banking apps can scan directly.
        String qrImageUrl = buildVietQrImageUrl(amount, transferContent);

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

        log.debug("Generated VietQR for payment {}: bankCode={}, amount={}, template={}",
                payment.getId(), bankCode, amount.longValue(), qrTemplate);

        return BankTransferQrResponse.builder()
                .paymentId(payment.getId())
                .paymentCode(paymentCode)
                .amount(amount)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .transferContent(transferContent)
                .qrContent(qrImageUrl)          // VietQR image URL — can be rendered by QRCodeSVG or displayed as <img>
                .qrImage(qrImageUrl)            // VietQR image URL — frontend displays as <img> for banking apps
                .expiresAt(payment.getExpiresAt())
                .status(status)
                .build();
    }

    /**
     * Builds a VietQR image API URL.
     * The returned URL points to a PNG image that encodes a VietQR-compliant
     * QR code with bank account, amount, and transfer content already embedded.
     *
     * URL format:
     *   https://img.vietqr.io/image/{bankCode}-{accountNumber}-{template}.png
     *     ?amount={amount}&addInfo={transferContent}&accountName={accountHolder}
     *
     * @param amount      transfer amount in VND
     * @param description transfer content (payment code + note)
     * @return fully qualified VietQR image URL
     */
    private String buildVietQrImageUrl(BigDecimal amount, String description) {
        String encodedAccountHolder = URLEncoder.encode(accountHolder, StandardCharsets.UTF_8);
        String encodedAddInfo = URLEncoder.encode(description, StandardCharsets.UTF_8);

        return String.format("%s/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s",
                VIETQR_IMAGE_BASE,
                bankCode,
                accountNumber,
                qrTemplate,
                amount.longValue(),
                encodedAddInfo,
                encodedAccountHolder);
    }
}
