package com.owlexa.owlexabackend.modules.payment.controller;

import com.owlexa.owlexabackend.modules.payment.dto.request.InstallmentRequest;
import com.owlexa.owlexabackend.modules.payment.dto.request.InstallmentScheduleRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.InstallmentResponse;
import com.owlexa.owlexabackend.modules.payment.service.InstallmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InstallmentController {

    private final InstallmentService installmentService;

    @PostMapping("/owner/fee-record/{feeRecordId}/installments")
    @PreAuthorize("hasAuthority('FEE_GENERATE')")
    public List<InstallmentResponse> createSchedule(@PathVariable Long feeRecordId,
                                                     @Valid @RequestBody InstallmentScheduleRequest request) {
        return installmentService.createSchedule(feeRecordId, request);
    }

    @GetMapping({"/owner/fee-record/{feeRecordId}/installments", "/cashier/fee-record/{feeRecordId}/installments"})
    @PreAuthorize("hasAnyAuthority('FEE_VIEW', 'PAYMENT_VIEW')")
    public List<InstallmentResponse> findByFeeRecord(@PathVariable Long feeRecordId) {
        return installmentService.findByFeeRecord(feeRecordId);
    }

    @PutMapping("/owner/installments/{installmentId}")
    @PreAuthorize("hasAuthority('FEE_GENERATE')")
    public InstallmentResponse update(@PathVariable Long installmentId,
                                       @Valid @RequestBody InstallmentRequest request) {
        return installmentService.updateInstallment(installmentId, request);
    }

    @DeleteMapping("/owner/installments/{installmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FEE_GENERATE')")
    public void delete(@PathVariable Long installmentId) {
        installmentService.deleteInstallment(installmentId);
    }
}
