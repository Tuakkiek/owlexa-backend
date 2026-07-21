package com.owlexa.owlexabackend.modules.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Component
public class SePayWebhookVerifier {

    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${sepay.webhook.secret}")
    private String webhookSecret;

    @Value("${sepay.webhook.timestamp-tolerance-seconds:300}")
    private long timestampToleranceSeconds;

    /**
     * @param rawBody          exact raw request body bytes as received, decoded as UTF-8 string
     * @param signatureHeader  value of header X-SePay-Signature, format "sha256={hex}"
     * @param timestampHeader  value of header X-SePay-Timestamp, unix seconds as string
     */
    public VerificationResult verify(String rawBody, String signatureHeader, String timestampHeader) {
        if (signatureHeader == null || signatureHeader.isBlank()) {
            return VerificationResult.failure("Missing X-SePay-Signature header");
        }
        if (timestampHeader == null || timestampHeader.isBlank()) {
            return VerificationResult.failure("Missing X-SePay-Timestamp header");
        }

        long timestamp;
        try {
            timestamp = Long.parseLong(timestampHeader.trim());
        } catch (NumberFormatException e) {
            return VerificationResult.failure("Invalid X-SePay-Timestamp header");
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestamp) > timestampToleranceSeconds) {
            return VerificationResult.failure("Timestamp outside tolerance window (possible replay)");
        }

        String expectedHex;
        try {
            String signingInput = timestamp + "." + rawBody;
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hash = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            expectedHex = bytesToHex(hash);
        } catch (Exception e) {
            return VerificationResult.failure("Failed to compute HMAC: " + e.getMessage());
        }

        String expectedHeader = "sha256=" + expectedHex;

        if (!constantTimeEquals(expectedHeader, signatureHeader.trim())) {
            return VerificationResult.failure("Signature mismatch");
        }

        return VerificationResult.success();
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(aBytes, bBytes);
    }

    public record VerificationResult(boolean valid, String failureReason) {
        public static VerificationResult success() {
            return new VerificationResult(true, null);
        }
        public static VerificationResult failure(String reason) {
            return new VerificationResult(false, reason);
        }
    }
}
