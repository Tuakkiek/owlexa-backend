-- V24: AI Drift Analytics columns on analytics_rubric_weakness

ALTER TABLE analytics_rubric_weakness
    ADD COLUMN ai_average_score   DOUBLE,
    ADD COLUMN teacher_average_score DOUBLE,
    ADD COLUMN drift_rate         DOUBLE;
