package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.modules.payment.dto.response.BankTransferQrResponse;
import com.owlexa.owlexabackend.modules.payment.entity.Payment;
import com.owlexa.owlexabackend.modules.payment.entity.TransactionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BankTransferQrServiceTest {

    private BankTransferQrService bankTransferQrService;

    @BeforeEach
    void setUp() {
        bankTransferQrService = new BankTransferQrService();
        org.springframework.test.util.ReflectionTestUtils.setField(bankTransferQrService, "bankName", "MB Bank");
        org.springframework.test.util.ReflectionTestUtils.setField(bankTransferQrService, "bankCode", "MB");
        org.springframework.test.util.ReflectionTestUtils.setField(bankTransferQrService, "accountNumber", "70740011223344");
        org.springframework.test.util.ReflectionTestUtils.setField(bankTransferQrService, "accountHolder", "Owlexa English Center");
        org.springframework.test.util.ReflectionTestUtils.setField(bankTransferQrService, "qrTemplate", "compact2");
    }

    @Test
    @DisplayName("buildQrResponse: transferContent should be exactly paymentCode without additional suffix")
    void buildQrResponse_transferContentExactMatch() {
        Payment payment = Payment.builder()
                .id(24L)
                .sepayRef("OWX000024")
                .amount(new BigDecimal("4500000"))
                .status(TransactionStatus.PENDING)
                .build();

        BankTransferQrResponse response = bankTransferQrService.buildQrResponse(payment);

        assertThat(response).isNotNull();
        assertThat(response.getPaymentCode()).isEqualTo("OWX000024");
        assertThat(response.getTransferContent()).isEqualTo("OWX000024");
        assertThat(response.getQrImage()).contains("addInfo=OWX000024");
        assertThat(response.getTransferContent()).doesNotContain("dong hoc phi");
    }
}
