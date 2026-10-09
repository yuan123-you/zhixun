-- One-time schema-first migration. Verify the column is absent before execution.
-- Legacy dates/statuses are NOT proof of approval; do not fabricate review permission.
ALTER TABLE cms_article ADD COLUMN review_state VARCHAR(16) NOT NULL DEFAULT 'UNVERIFIED';
