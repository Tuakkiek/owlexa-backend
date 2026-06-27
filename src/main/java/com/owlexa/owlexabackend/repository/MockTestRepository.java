package com.owlexa.owlexabackend.repository;

import com.owlexa.owlexabackend.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockTestRepository extends JpaRepository<MockTest, Long> {
    List<MockTest> findAllByCenterIdOrderByCreatedAtDesc(Long centerId);
}
