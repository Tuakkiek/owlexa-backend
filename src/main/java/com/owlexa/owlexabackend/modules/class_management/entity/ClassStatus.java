package com.owlexa.owlexabackend.modules.class_management.entity;

public enum ClassStatus {
    PLANNED,     // Class created, not yet started
    ACTIVE,      // Class is currently running (enrollment, attendance, teaching)
    FINISHED     // Class has completed
}
