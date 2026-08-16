package com.owlexa.owlexabackend.modules.payment.controller;
import com.owlexa.owlexabackend.modules.payment.dto.request.CashierRequest;
import com.owlexa.owlexabackend.modules.payment.dto.response.CashierResponse;
import com.owlexa.owlexabackend.modules.payment.service.CashierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/cashiers")
@RequiredArgsConstructor
public class CashierController {

    private final CashierService cashierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public CashierResponse create(@RequestBody CashierRequest request) {
        return cashierService.create(request);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public List<CashierResponse> findAll() {
        return cashierService.findAll();
    }

    // Update Cashier
    @PutMapping("/{cashierId}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public CashierResponse update(@PathVariable Long cashierId,
                                  @Valid @RequestBody CashierRequest request) {
        return cashierService.update(cashierId, request);
    }
    // Delete Cashier
    @DeleteMapping("/{cashierId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('USER_DEACTIVATE')")
    public void delete(@PathVariable Long cashierId) {
        cashierService.delete(cashierId);
    }
}
