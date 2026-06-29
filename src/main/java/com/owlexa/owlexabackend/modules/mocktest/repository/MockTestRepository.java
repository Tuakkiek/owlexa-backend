package com.owlexa.owlexabackend.modules.mocktest.repository;
import com.owlexa.owlexabackend.modules.mocktest.entity.MockTest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MockTestRepository extends JpaRepository<MockTest, Long> {
    List<MockTest> findAllByCenterIdOrderByCreatedAtDesc(Long centerId);
}
