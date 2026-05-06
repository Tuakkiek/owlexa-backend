package com.owlexa.owlexabackend.controller;

import com.owlexa.owlexabackend.dto.request.CashierRequest;
import com.owlexa.owlexabackend.dto.response.CashierResponse;
import com.owlexa.owlexabackend.service.CashierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/owner/cashiers")
@RequiredArgsConstructor
public class CashierController {

    private final CashierService cashierService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CashierResponse create(@RequestBody CashierRequest request) {
        return cashierService.create(request);
    }

    @GetMapping
    public List<CashierResponse> findAll() {
        return cashierService.findAll();
    }
}
