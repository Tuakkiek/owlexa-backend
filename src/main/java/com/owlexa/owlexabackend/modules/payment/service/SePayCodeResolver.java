package com.owlexa.owlexabackend.modules.payment.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SePayCodeResolver {

    @Value("${sepay.payment-code.prefix:OWX}")
    private String prefix;

    /**
     * Extracts a paymentId from a payment code string that starts with the prefix.
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

        return extractNumericId(normalized.substring(prefixUpper.length()));
    }

    /**
     * Scans raw transfer content for the payment code prefix anywhere in the string.
     * Handles cases where the banking app embeds the payment code inside
     * a longer transfer description (e.g., "MBVCB.123.OWX000035thanh toan...").
     *
     * Uses regex to find the prefix followed by digits anywhere in the content.
     * Returns empty if no match is found.
     */
    public Optional<Long> resolveFromContent(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        String prefixUpper = prefix.toUpperCase();
        // Find {prefix} followed by one or more digits, case-insensitive
        Pattern pattern = Pattern.compile(
                Pattern.quote(prefixUpper) + "(\\d+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String numericPart = matcher.group(1);
        return extractNumericId(numericPart);
    }

    /**
     * Parses a numeric string (possibly with leading zeros) into a Long.
     */
    private Optional<Long> extractNumericId(String numericPart) {
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
