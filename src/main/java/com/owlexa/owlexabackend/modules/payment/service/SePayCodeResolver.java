package com.owlexa.owlexabackend.modules.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SePayCodeResolver {

    @Value("${sepay.payment-code.prefix:OWX}")
    private String prefix;

    /**
     * Extracts a paymentId from a payment code.
     * Supports both fixed-length format (e.g., "OWX000015" -> 15L)
     * and legacy variable-length format (e.g., "OWX15" -> 15L).
     * Returns empty if the code is null/blank or does not match the expected pattern.
     */
    public Optional<Long> resolvePaymentId(String paymentCode) {
        if (paymentCode == null || paymentCode.isBlank()) {
            return Optional.empty();
        }
        String normalized = paymentCode.trim().toUpperCase();
        String prefixUpper = prefix.toUpperCase();

        if (!normalized.startsWith(prefixUpper)) {
            return Optional.empty();
        }

        String numericPart = normalized.substring(prefixUpper.length());
        if (numericPart.isEmpty()) {
            return Optional.empty();
        }

        // Strip leading zeros for fixed-length format (e.g., "000015" -> "15")
        String stripped = numericPart.replaceFirst("^0+(?!$)", "");

        try {
            return Optional.of(Long.parseLong(stripped));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
