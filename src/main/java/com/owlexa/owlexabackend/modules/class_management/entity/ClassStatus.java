package com.owlexa.owlexabackend.modules.class_management.entity;

public enum ClassStatus {
    PLANNING,     // Class created, not yet open for enrollment
    OPEN,         // Enrollment allowed
    FULL,         // Enrollment full (optional waiting list)
    IN_PROGRESS,  // Teaching phase, attendance active
    FINISHED,     // Teaching complete
    ARCHIVED,     // Hidden from default views
    CANCELLED     // Cancelled before starting
}
