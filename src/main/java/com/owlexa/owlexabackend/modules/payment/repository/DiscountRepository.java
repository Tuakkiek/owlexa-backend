package com.owlexa.owlexabackend.modules.payment.repository;

import com.owlexa.owlexabackend.modules.payment.entity.Discount;
import com.owlexa.owlexabackend.modules.payment.entity.FeeRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscountRepository extends JpaRepository<Discount, Long> {

    List<Discount> findAllByFeeRecordOrderByCreatedAtDesc(FeeRecord feeRecord);

    List<Discount> findAllByFeeRecord_Id(Long feeRecordId);
}
