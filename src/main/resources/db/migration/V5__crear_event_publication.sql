CREATE TABLE event_publication (
  id UUID PRIMARY KEY,
  publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
  listener_id VARCHAR(255) NOT NULL,
  serialized_event TEXT NOT NULL,
  event_type VARCHAR(255) NOT NULL,
  completion_date TIMESTAMP WITH TIME ZONE,
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  completion_attempts INTEGER NOT NULL DEFAULT 0,
  status VARCHAR(32) NOT NULL
);

CREATE INDEX ix_event_publication_incomplete
  ON event_publication (completion_date, publication_date)
  WHERE completion_date IS NULL;
