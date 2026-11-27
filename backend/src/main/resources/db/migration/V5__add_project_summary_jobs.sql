-- Speichert die Projekt-Summary und ihren Erstellungsstatus direkt am Projekt.
ALTER TABLE projects ADD COLUMN summary TEXT;
ALTER TABLE projects ADD COLUMN summary_status VARCHAR(20) NOT NULL DEFAULT 'IDLE';
ALTER TABLE projects ADD COLUMN summary_request_id UUID;