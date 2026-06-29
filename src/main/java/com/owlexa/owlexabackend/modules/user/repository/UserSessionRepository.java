package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    boolean existsByIdAndActiveTrue(String id);

    List<UserSession> findByUser_IdAndActiveTrueOrderByLastUsedAtDesc(Long userId);

    Optional<UserSession> findByIdAndUser_Id(String sessionId, Long userId);

    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.active = false WHERE s.user.id = :userId AND s.active = true")
    void deactivateAllByUserId(@Param("userId") Long userId);
}