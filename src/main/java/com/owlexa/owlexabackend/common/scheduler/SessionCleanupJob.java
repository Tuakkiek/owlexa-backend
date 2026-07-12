package com.owlexa.owlexabackend.common.scheduler;

import com.owlexa.owlexabackend.modules.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Daily cleanup job that hard-deletes stale session rows.
 *
 * <p>Three cleanup targets:
 * <ol>
 *   <li>Inactive sessions (active=false) older than retention period</li>
 *   <li>Expired sessions that slipped past deactivation (safety net)</li>
 *   <li>Revoked sessions older than retention period</li>
 * </ol>
 *
 * <p>Uses hard-delete (not soft-delete) because:
 * <ul>
 *   <li>Sessions are not audit-critical after >30 days of inactivity</li>
 *   <li>Soft-delete would still bloat the table and slow down JwtFilter lookups</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionCleanupJob {

    private final UserSessionRepository sessionRepository;

    @Value("${app.session.cleanup-retention-days:30}")
    private int retentionDays;

    /**
     * Runs daily at configured cron (default: 3:00 AM).
     * Disabled when cron is "-" (e.g. in tests).
     */
    @Scheduled(cron = "${app.session.cleanup-cron:0 0 3 * * *}")
    public void cleanupStaleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        int inactiveDeleted = sessionRepository.deleteInactiveOlderThan(cutoff);
        int expiredDeleted  = sessionRepository.deleteExpiredOlderThan(cutoff);
        int revokedDeleted  = sessionRepository.deleteRevokedOlderThan(cutoff);

        int total = inactiveDeleted + expiredDeleted + revokedDeleted;
        if (total > 0) {
            log.info("Session cleanup: deleted {} stale sessions (inactive={}, expired={}, revoked={})",
                    total, inactiveDeleted, expiredDeleted, revokedDeleted);
        }
    }
}
