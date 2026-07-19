package com.owlexa.owlexabackend.modules.payment.scheduler;

import com.owlexa.owlexabackend.modules.payment.entity.Installment;
import com.owlexa.owlexabackend.modules.payment.entity.InstallmentStatus;
import com.owlexa.owlexabackend.modules.payment.repository.InstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Daily job: marks PENDING or PARTIALLY_PAID installments as OVERDUE
 * when their due date has passed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InstallmentOverdueJob {

    private final InstallmentRepository installmentRepository;

    @Scheduled(cron = "0 0 1 * * *") // 1 AM daily
    @Transactional
    public void markOverdueInstallments() {
        LocalDate today = LocalDate.now();
        List<Installment> overdue = installmentRepository.findAll().stream()
                .filter(i -> i.getDueDate() != null && i.getDueDate().isBefore(today))
                .filter(i -> i.getStatus() == InstallmentStatus.PENDING
                        || i.getStatus() == InstallmentStatus.PARTIALLY_PAID)
                .toList();

        if (overdue.isEmpty()) return;

        for (Installment inst : overdue) {
            inst.setStatus(InstallmentStatus.OVERDUE);
            installmentRepository.save(inst);
        }
        log.info("InstallmentOverdueJob: marked {} installments as OVERDUE", overdue.size());
    }
}
