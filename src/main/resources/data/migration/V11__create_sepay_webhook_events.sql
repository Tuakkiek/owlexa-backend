CREATE TABLE sepay_webhook_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sepay_transaction_id BIGINT NOT NULL,
    reference_code VARCHAR(64),
    gateway VARCHAR(64),
    account_number VARCHAR(32),
    sub_account VARCHAR(64),
    payment_code VARCHAR(64),
    content VARCHAR(500),
    transfer_type VARCHAR(8),
    transfer_amount BIGINT,
    transaction_date VARCHAR(32),
    raw_payload TEXT,
    processing_status VARCHAR(16) NOT NULL,
    processing_note VARCHAR(1000),
    matched_payment_id BIGINT,
    received_at DATETIME NOT NULL,
    processed_at DATETIME,
    CONSTRAINT uk_sepay_event_id UNIQUE (sepay_transaction_id)
);

CREATE INDEX idx_sepay_events_payment_code ON sepay_webhook_events (payment_code);
CREATE INDEX idx_sepay_events_status ON sepay_webhook_events (processing_status);
