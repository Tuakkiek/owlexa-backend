package com.owlexa.owlexabackend.modules.payment.entity;

public enum SePayEventStatus {
    RECEIVED,       // signature verified, stored, not yet processed
    MATCHED,        // payment code resolved, Payment confirmed
    UNMATCHED,      // no valid payment code / no matching Payment found
    IGNORED,        // transferType == "out", or duplicate, or filtered
    FAILED          // unexpected error during processing
}
