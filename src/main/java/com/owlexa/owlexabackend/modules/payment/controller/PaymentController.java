package com.owlexa.owlexabackend.modules.payment.controller;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashPaymentRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.PaymentResponse;
import com.owlexa.owlexabackend.modules.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping({
            "/owner/fee-record/{feeRecordId}/payments/cash",
            "/cashier/fee-record/{feeRecordId}/payments/cash"
    })
    @ResponseStatus(HttpStatus.CREATED)
    public PaymentResponse collectCash(
            @PathVariable Long feeRecordId,
            @Valid @RequestBody CashPaymentRequest request
    ) {
        return paymentService.collectCash(feeRecordId, request);
    }

    @GetMapping({
            "/owner/fee-record/{feeRecordId}/payments",
            "/cashier/fee-record/{feeRecordId}/payments"
    })
    public List<PaymentResponse> findAllByFeeRecord(
            @PathVariable Long feeRecordId
    ) {
        return paymentService.findAllByFeeRecord(feeRecordId);
    }

    @GetMapping("/student/payments/me")
    public List<PaymentResponse> findMyPayment() {
        return paymentService.findMyPayments();
    }

    @GetMapping({
            "/owner/payments",
            "/cashier/payments"
    })
    public List<PaymentResponse> findAllByCenter() {
        return paymentService.findAllByCenter();
    }
}
