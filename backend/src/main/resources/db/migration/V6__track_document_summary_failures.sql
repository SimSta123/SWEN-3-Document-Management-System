ALTER TABLE documents ADD COLUMN summary_failed BOOLEAN NOT NULL DEFAULT FALSE;

-- Older unsummarized PDFs have no guaranteed summary job after upgrade.
UPDATE documents
SET summary_failed = TRUE
WHERE object_key IS NOT NULL AND (summary IS NULL OR TRIM(summary) = '');

-- Let users retry previous requests that were waiting on those PDFs.
UPDATE projects SET summary_status = 'FAILED' WHERE summary_status = 'WAITING';
