CREATE TABLE event_publication (
  id UUID PRIMARY KEY,
  completion_attempts INTEGER,
  completion_date TIMESTAMP WITH TIME ZONE,
  event_type VARCHAR(512),
  last_resubmission_date TIMESTAMP WITH TIME ZONE,
  listener_id VARCHAR(512),
  publication_date TIMESTAMP WITH TIME ZONE,
  serialized_event TEXT,
  status VARCHAR(64)
);
