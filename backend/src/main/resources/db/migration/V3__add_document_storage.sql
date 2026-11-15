ALTER TABLE documents ADD COLUMN object_key VARCHAR(512);
ALTER TABLE documents ADD COLUMN original_filename VARCHAR(255);
ALTER TABLE documents ADD COLUMN content_type VARCHAR(100);
ALTER TABLE documents ADD COLUMN size_bytes BIGINT;
