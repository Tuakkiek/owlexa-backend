package com.owlexa.owlexabackend.modules.payment.repository;

import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import com.owlexa.owlexabackend.modules.payment.entity.Installment;
import com.owlexa.owlexabackend.modules.payment.entity.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface InstallmentRepository extends JpaRepository<Installment, Long> {

    List<Installment> findAllByFeeRecordOrderByDueDateAsc(FeeRecord feeRecord);

    List<Installment> findAllByFeeRecord_IdOrderByDueDateAsc(Long feeRecordId);

    @Query("SELECT i FROM Installment i WHERE i.feeRecord = :feeRecord AND i.status IN :statuses ORDER BY i.dueDate ASC")
    List<Installment> findOldestUnpaid(@Param("feeRecord") FeeRecord feeRecord,
                                        @Param("statuses") List<InstallmentStatus> statuses);

    @Query("SELECT COUNT(i) FROM Installment i WHERE i.center.id = :centerId AND i.dueDate = :today AND i.status IN :statuses")
    long countByCenterIdAndDueDateAndStatusIn(@Param("centerId") Long centerId,
                                               @Param("today") LocalDate today,
                                               @Param("statuses") List<InstallmentStatus> statuses);
}
