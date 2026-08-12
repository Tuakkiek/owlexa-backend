package com.owlexa.owlexabackend.modules.payment.controller;

import com.owlexa.owlexabackend.modules.payment.dto.request.RefundDecisionRequest;
import com.owlexa.owlexabackend.modules.payment.dto.request.RefundPayoutRequest;
import com.owlexa.owlexabackend.modules.payment.dto.request.RefundRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.RefundResponse;
import com.owlexa.owlexabackend.modules.payment.entity.RefundStatus;
import com.owlexa.owlexabackend.modules.payment.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/owner/refunds", "/cashier/refunds"})
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyAuthority('REFUND_REQUEST', 'PAYMENT_REFUND')")
    public RefundResponse requestRefund(@Valid @RequestBody RefundRequest request) {
        return refundService.requestRefund(request);
    }

    @PatchMapping("/{refundId}/decision")
    @PreAuthorize("hasAnyAuthority('REFUND_APPROVE', 'PAYMENT_REFUND')")
    public RefundResponse decideRefund(
            @PathVariable Long refundId,
            @Valid @RequestBody RefundDecisionRequest request) {
        return refundService.decide(refundId, request);
    }

    @PatchMapping("/{refundId}/payout")
    @PreAuthorize("hasAnyAuthority('REFUND_PAY', 'PAYMENT_REFUND')")
    public RefundResponse markPaid(
            @PathVariable Long refundId,
            @Valid @RequestBody RefundPayoutRequest request) {
        return refundService.markPaid(refundId, request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('REFUND_APPROVE', 'PAYMENT_REFUND', 'PAYMENT_VIEW')")
    public List<RefundResponse> getRefunds(
            @RequestParam(required = false) RefundStatus status) {
        return refundService.findAll(status);
    }
}
