ALTER TABLE bookings ALTER COLUMN status DROP DEFAULT;

ALTER TYPE booking_status RENAME TO booking_status_old;

CREATE TYPE booking_status AS ENUM (
  'DRAFT',
  'CONFIRMED',
  'IN_PROGRESS',
  'COMPLETED',
  'REJECTED',
  'CANCELLED'
);

ALTER TABLE bookings
  ALTER COLUMN status TYPE booking_status
  USING (
    CASE status::text
      WHEN 'CHECKED_IN' THEN 'IN_PROGRESS'
      WHEN 'NO_SHOW' THEN 'REJECTED'
      ELSE status::text
    END
  )::booking_status;

ALTER TABLE bookings
  ALTER COLUMN status SET DEFAULT 'DRAFT'::booking_status;

ALTER TABLE booking_status_events
  ALTER COLUMN from_status TYPE booking_status
  USING (
    CASE from_status::text
      WHEN 'CHECKED_IN' THEN 'IN_PROGRESS'
      WHEN 'NO_SHOW' THEN 'REJECTED'
      ELSE from_status::text
    END
  )::booking_status;

ALTER TABLE booking_status_events
  ALTER COLUMN to_status TYPE booking_status
  USING (
    CASE to_status::text
      WHEN 'CHECKED_IN' THEN 'IN_PROGRESS'
      WHEN 'NO_SHOW' THEN 'REJECTED'
      ELSE to_status::text
    END
  )::booking_status;

DROP TYPE booking_status_old;
