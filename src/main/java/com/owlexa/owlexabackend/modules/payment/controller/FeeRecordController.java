package com.owlexa.owlexabackend.modules.payment.controller;
import com.owlexa.owlexabackend.modules.payment.dto.request.FeeRecordGenerateRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.FeeRecordResponse;
import com.owlexa.owlexabackend.modules.payment.service.FeeRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeeRecordController {

    private final FeeRecordService feeRecordService;

    @PostMapping("/owner/classes/{classId}/fee-records/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEE_GENERATE')")
    public List<FeeRecordResponse> generateForClass(
            @PathVariable Long classId,
            @Valid @RequestBody FeeRecordGenerateRequest request
    ) {
        return feeRecordService.generateForClass(classId, request);
    }

    @GetMapping("/owner/classes/{classId}/fee-records")
    @PreAuthorize("hasAnyAuthority('FEE_VIEW', 'PAYMENT_VIEW')")
    public List<FeeRecordResponse> findAllByClass(
            @PathVariable Long classId,
            @RequestParam String month
    ) {
        return feeRecordService.findAllByClass(classId, month);
    }

    @GetMapping("/fee-records/me")
    public List<FeeRecordResponse> findMyFees() {
        return feeRecordService.findMyFees();
    }

    @GetMapping({"/owner/fee-records/overdue", "/cashier/fee-records/overdue"})
    @PreAuthorize("hasAnyAuthority('FEE_VIEW', 'PAYMENT_VIEW')")
    public List<FeeRecordResponse> findAllOverdue() {
        return feeRecordService.findAllOverdue();
    }
}