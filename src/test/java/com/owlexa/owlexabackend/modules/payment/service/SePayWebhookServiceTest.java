package com.owlexa.owlexabackend.modules.payment.service;

import com.owlexa.owlexabackend.common.exception.BusinessRuleException;
import com.owlexa.owlexabackend.modules.payment.dto.request.SePayWebhookRequest;
import com.owlexa.owlexabackend.modules.payment.entity.SePayEventStatus;
import com.owlexa.owlexabackend.modules.payment.entity.SePayWebhookEvent;
import com.owlexa.owlexabackend.modules.payment.repository.SePayWebhookEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class SePayWebhookServiceTest {

    @Mock private SePayWebhookEventRepository eventRepository;
    @Mock private SePayCodeResolver codeResolver;
    @Mock private PaymentService paymentService;

    @Test
    @DisplayName("processWebhook: giao dịch ngân hàng lần hai cùng QR → DUPLICATE_PAYMENT")
    void processWebhook_whenPaymentServiceReportsDuplicate_shouldMarkDuplicatePayment() {
        SePayWebhookService service = new SePayWebhookService(eventRepository, codeResolver, paymentService);
        SePayWebhookRequest request = new SePayWebhookRequest();
        request.setId(7001L);
        request.setTransferType("in");
        request.setCode("OWX000090");
        request.setTransferAmount(1000L);

        when(eventRepository.saveAndFlush(any(SePayWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(SePayWebhookEvent.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(codeResolver.resolvePaymentId("OWX000090")).thenReturn(Optional.of(90L));
        doThrow(new BusinessRuleException(PaymentService.DUPLICATE_PAYMENT_CODE,
                "DUPLICATE_PAYMENT: payment 90 was already confirmed"))
                .when(paymentService).confirmBankTransferPayment(90L, request);

        SePayWebhookEvent event = service.processWebhook(request, "{}");

        assertThat(event.getMatchedPaymentId()).isEqualTo(90L);
        assertThat(event.getProcessingStatus()).isEqualTo(SePayEventStatus.DUPLICATE_PAYMENT);
        assertThat(event.getProcessingNote()).contains("DUPLICATE_PAYMENT");
    }
}
