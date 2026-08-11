package com.owlexa.owlexabackend.modules.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.owlexa.owlexabackend.modules.payment.dto.request.SePayWebhookRequest;
import com.owlexa.owlexabackend.modules.payment.entity.SePayWebhookEvent;
import com.owlexa.owlexabackend.modules.payment.service.SePayWebhookService;
import com.owlexa.owlexabackend.modules.payment.service.SePayWebhookVerifier;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/webhooks/sepay")
@RequiredArgsConstructor
@Slf4j
public class SePayWebhookController {

    private final SePayWebhookVerifier verifier;
    private final SePayWebhookService webhookService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            HttpServletRequest request,
            @RequestHeader(value = "X-SePay-Signature", required = false) String signatureHeader,
            @RequestHeader(value = "X-SePay-Timestamp", required = false) String timestampHeader
    ) {
        // ── DIAGNOSTIC: Prove the request reached the controller ──
        // Uses INFO level so it's always visible regardless of log config.
        // If this log NEVER appears, the request is blocked BEFORE the controller
        // (ngrok down, wrong URL, firewall, or Spring Security).
        log.info("[SEPAY-WEBHOOK-DIAG] >>> REQUEST RECEIVED <<< remoteAddr={}, method={}, uri={}, contentType={}",
                request.getRemoteAddr(), request.getMethod(), request.getRequestURI(),
                request.getContentType());
        log.info("[SEPAY-WEBHOOK-DIAG] Headers: X-SePay-Signature={}, X-SePay-Timestamp={}, Host={}, User-Agent={}",
                signatureHeader != null ? signatureHeader.substring(0, Math.min(30, signatureHeader.length())) + "..." : "NULL",
                timestampHeader,
                request.getHeader("Host"),
                request.getHeader("User-Agent"));
        // ── END DIAGNOSTIC ──

        // ── TEMPORARY DEBUG LOGGING (remove for production) ──
        log.debug("[SEPAY-WEBHOOK] Received request from {}", request.getRemoteAddr());
        log.debug("[SEPAY-WEBHOOK] X-SePay-Timestamp: {}", timestampHeader);
        log.debug("[SEPAY-WEBHOOK] X-SePay-Signature (first 20 chars): {}...",
                signatureHeader != null && signatureHeader.length() > 20
                        ? signatureHeader.substring(0, 20)
                        : signatureHeader);
        // ── END TEMPORARY DEBUG ──

        String rawBody;
        try {
            rawBody = readRawBody(request);
            // ── TEMPORARY DEBUG LOGGING ──
            log.debug("[SEPAY-WEBHOOK] Raw body ({} chars): {}",
                    rawBody.length(),
                    rawBody.length() > 500 ? rawBody.substring(0, 500) + "..." : rawBody);
            // ── END TEMPORARY DEBUG ──
        } catch (Exception e) {
            log.error("Failed to read SePay webhook body", e);
            return ResponseEntity.badRequest()
                    .body(Map.<String, Object>of("success", false, "message", "Unable to read request body"));
        }

        var verification = verifier.verify(rawBody, signatureHeader, timestampHeader);
        if (!verification.valid()) {
            log.warn("[SEPAY-WEBHOOK] Rejected SePay webhook: {}", verification.failureReason());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.<String, Object>of("success", false, "message", "Invalid signature"));
        }
        // ── TEMPORARY DEBUG LOGGING ──
        log.debug("[SEPAY-WEBHOOK] HMAC signature VERIFIED successfully");
        // ── END TEMPORARY DEBUG ──

        SePayWebhookRequest payload;
        try {
            payload = objectMapper.readValue(rawBody, SePayWebhookRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse SePay webhook payload", e);
            return ResponseEntity.badRequest()
                    .body(Map.<String, Object>of("success", false, "message", "Invalid payload"));
        }

        // ── TEMPORARY DEBUG LOGGING ──
        log.debug("[SEPAY-WEBHOOK] Parsed payload: transferType={}, code={}, transferAmount={}, id={}",
                payload.getTransferType(), payload.getCode(), payload.getTransferAmount(), payload.getId());
        // ── END TEMPORARY DEBUG ──

        SePayWebhookEvent event = webhookService.processWebhook(payload, rawBody);

        // Always return 200 once signature is valid and event is durably stored,
        // even if downstream matching failed — prevents SePay from retrying
        // an event that was already logged for manual review.
        var responseBody = Map.<String, Object>of(
                "success", true,
                "eventId", event.getId(),
                "status", event.getProcessingStatus().name()
        );

        // ── TEMPORARY DEBUG LOGGING ──
        log.debug("[SEPAY-WEBHOOK] Final result: sepayTxId={}, paymentCode={}, status={}, note={}",
                event.getSepayTransactionId(), event.getPaymentCode(),
                event.getProcessingStatus(), event.getProcessingNote());
        // ── END TEMPORARY DEBUG ──

        return ResponseEntity.ok(responseBody);
    }

    private String readRawBody(HttpServletRequest request) throws Exception {
        try (var inputStream = request.getInputStream();
             var buffer = new ByteArrayOutputStream()) {
            byte[] chunk = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            return buffer.toString(StandardCharsets.UTF_8);
        }
    }
}
