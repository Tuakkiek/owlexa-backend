ALTER TABLE `sepay_webhook_events`
    MODIFY `processing_status` ENUM(
        'FAILED',
        'IGNORED',
        'MATCHED',
        'RECEIVED',
        'UNMATCHED',
        'DUPLICATE_PAYMENT'
    ) NOT NULL;
