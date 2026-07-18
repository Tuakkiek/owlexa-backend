package com.owlexa.owlexabackend.modules.user.controller;

import com.owlexa.owlexabackend.modules.user.dto.request.ChangePasswordRequest;
import com.owlexa.owlexabackend.modules.user.dto.request.UpdateAccountRequest;
import com.owlexa.owlexabackend.modules.user.dto.response.AccountResponse;
import com.owlexa.owlexabackend.modules.user.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<AccountResponse> getMyAccount() {
        return ResponseEntity.ok(accountService.getMyAccount());
    }

    @PutMapping
    public ResponseEntity<AccountResponse> updateMyAccount(
            @Valid @RequestBody UpdateAccountRequest request) {
        return ResponseEntity.ok(accountService.updateMyAccount(request));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changeMyPassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        accountService.changeMyPassword(request);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
