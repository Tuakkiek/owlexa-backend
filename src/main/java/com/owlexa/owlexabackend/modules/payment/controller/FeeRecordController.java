package com.owlexa.owlexabackend.modules.payment.controller;
import com.owlexa.owlexabackend.modules.payment.dto.request.UpdateDueDateRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.FeeRecordResponse;
import com.owlexa.owlexabackend.modules.payment.service.FeeRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeeRecordController {

    private final FeeRecordService feeRecordService;

    @GetMapping("/fee-records/me")
    public List<FeeRecordResponse> findMyFees() {
        return feeRecordService.findMyFees();
    }

    @GetMapping({"/owner/fee-records/overdue", "/cashier/fee-records/overdue"})
    @PreAuthorize("hasAnyAuthority('FEE_VIEW', 'PAYMENT_VIEW', 'CASHIER_PAYMENTS')")
    public List<FeeRecordResponse> findAllOverdue() {
        return feeRecordService.findAllOverdue();
    }

    @GetMapping({"/owner/fee-records/pending", "/cashier/fee-records/pending"})
    @PreAuthorize("hasAnyAuthority('FEE_VIEW', 'PAYMENT_VIEW', 'CASHIER_PAYMENTS')")
    public List<FeeRecordResponse> findAllPending() {
        return feeRecordService.findAllPending();
    }

    @GetMapping({"/owner/classes/{classId}/fee-records", "/cashier/classes/{classId}/fee-records"})
    @PreAuthorize("hasAnyAuthority('FEE_VIEW', 'PAYMENT_VIEW', 'CASHIER_PAYMENTS')")
    public List<FeeRecordResponse> findByClass(@PathVariable Long classId) {
        return feeRecordService.findByClass(classId);
    }

    @PutMapping({"/owner/classes/{classId}/fee-records/due-date", "/cashier/classes/{classId}/fee-records/due-date"})
    @PreAuthorize("hasAnyAuthority('FEE_GENERATE', 'PAYMENT_COLLECT', 'CASHIER_PAYMENTS')")
    public List<FeeRecordResponse> updateClassFeeDueDate(@PathVariable Long classId,
                                                          @Valid @RequestBody UpdateDueDateRequest request) {
        return feeRecordService.updateClassFeeDueDate(classId, request);
    }
}
