package com.owlexa.owlexabackend.modules.user.repository;
import com.owlexa.owlexabackend.modules.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    boolean existsByIdAndActiveTrue(String id);

    Optional<UserSession> findByIdAndActiveTrue(String id);

    List<UserSession> findByUser_IdAndActiveTrueOrderByLastUsedAtDesc(Long userId);

    Optional<UserSession> findByIdAndUser_Id(String sessionId, Long userId);

    // ── Device dedup ─────────────────────────────────────────────────────

    /** Find active session for a user on a specific device (by deviceKey). */
    Optional<UserSession> findByUser_IdAndDeviceKeyAndActiveTrue(Long userId, String deviceKey);

    // ── Multi-device limit ───────────────────────────────────────────────

    /** Count active sessions for a user. */
    long countByUser_IdAndActiveTrue(Long userId);

    /**
     * Find the oldest active session for a user (by lastUsedAt).
     * Used to evict when exceeding max-devices limit.
     */
    Optional<UserSession> findFirstByUser_IdAndActiveTrueOrderByLastUsedAtAsc(Long userId);

    // ── Mass deactivation (with audit) ───────────────────────────────────

    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.active = false, s.revokedReason = :reason, s.revokedAt = CURRENT_TIMESTAMP " +
           "WHERE s.user.id = :userId AND s.active = true")
    void deactivateAllByUserIdWithReason(@Param("userId") Long userId, @Param("reason") String reason);

    /** Legacy — delegates to {@link #deactivateAllByUserIdWithReason} with REUSE_DETECTED. */
    @Modifying
    @Transactional
    @Query("UPDATE UserSession s SET s.active = false WHERE s.user.id = :userId AND s.active = true")
    void deactivateAllByUserId(@Param("userId") Long userId);

    // ── Cleanup ──────────────────────────────────────────────────────────

    /** Hard-delete sessions that have been inactive (active=false) for longer than cutoff. */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.active = false AND s.lastUsedAt < :cutoff")
    int deleteInactiveOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** Hard-delete expired sessions (safety net for any that slipped past deactivation). */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.inactiveExpireAt < :cutoff")
    int deleteExpiredOlderThan(@Param("cutoff") LocalDateTime cutoff);

    /** Hard-delete revoked sessions older than cutoff. */
    @Modifying
    @Transactional
    @Query("DELETE FROM UserSession s WHERE s.revokedAt IS NOT NULL AND s.revokedAt < :cutoff")
    int deleteRevokedOlderThan(@Param("cutoff") LocalDateTime cutoff);
}