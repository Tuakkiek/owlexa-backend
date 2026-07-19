package com.owlexa.owlexabackend.modules.payment.controller;

import com.owlexa.owlexabackend.modules.payment.dto.request.DiscountRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.DiscountResponse;
import com.owlexa.owlexabackend.modules.payment.service.DiscountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DiscountController {

    private final DiscountService discountService;

    @PostMapping("/owner/fee-record/{feeRecordId}/discounts")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('FEE_GENERATE')")
    public DiscountResponse create(@PathVariable Long feeRecordId,
                                    @Valid @RequestBody DiscountRequest request) {
        return discountService.create(feeRecordId, request);
    }

    @GetMapping({"/owner/fee-record/{feeRecordId}/discounts", "/cashier/fee-record/{feeRecordId}/discounts"})
    @PreAuthorize("hasAnyAuthority('FEE_VIEW', 'PAYMENT_VIEW')")
    public List<DiscountResponse> findByFeeRecord(@PathVariable Long feeRecordId) {
        return discountService.findByFeeRecord(feeRecordId);
    }

    @PutMapping("/owner/discounts/{discountId}")
    @PreAuthorize("hasAuthority('FEE_GENERATE')")
    public DiscountResponse update(@PathVariable Long discountId,
                                    @Valid @RequestBody DiscountRequest request) {
        return discountService.update(discountId, request);
    }

    @DeleteMapping("/owner/discounts/{discountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('FEE_GENERATE')")
    public void delete(@PathVariable Long discountId) {
        discountService.delete(discountId);
    }
}
