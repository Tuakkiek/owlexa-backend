package com.owlexa.owlexabackend.modules.teacher.repository;

import com.owlexa.owlexabackend.modules.teacher.entity.TeacherCenterProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherCenterProfileRepository extends JpaRepository<TeacherCenterProfile, Long> {

    /**
     * Tìm salary theo teacher và center cụ thể.
     * Dùng để set/get salary cho một teacher tại một center.
     *
     * Unique ở mức DB nên kết quả chỉ có 0 hoặc 1.
     */
    Optional<TeacherCenterProfile> findByTeacher_IdAndCenter_Id(Long teacherId, Long centerId);

    boolean existsByTeacher_IdAndCenter_Id(Long teacherId, Long centerId);
}