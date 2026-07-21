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
        String rawBody;
        try {
            rawBody = readRawBody(request);
        } catch (Exception e) {
            log.error("Failed to read SePay webhook body", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Unable to read request body"));
        }

        var verification = verifier.verify(rawBody, signatureHeader, timestampHeader);
        if (!verification.valid()) {
            log.warn("Rejected SePay webhook: {}", verification.failureReason());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid signature"));
        }

        SePayWebhookRequest payload;
        try {
            payload = objectMapper.readValue(rawBody, SePayWebhookRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse SePay webhook payload", e);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Invalid payload"));
        }

        SePayWebhookEvent event = webhookService.processWebhook(payload, rawBody);

        // Always return 200 once signature is valid and event is durably stored,
        // even if downstream matching failed — prevents SePay from retrying
        // an event that was already logged for manual review.
        return ResponseEntity.ok(Map.of(
                "success", true,
                "eventId", event.getId(),
                "status", event.getProcessingStatus().name()
        ));
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
