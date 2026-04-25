DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM pg_enum enum_value
    JOIN pg_type enum_type ON enum_type.oid = enum_value.enumtypid
    WHERE enum_type.typname = 'booking_status'
      AND enum_value.enumlabel = 'IN_PROGRESS'
  ) THEN
    UPDATE bookings
    SET status = 'CHECKED_IN'::booking_status
    WHERE status = 'IN_PROGRESS'::booking_status;

    UPDATE booking_status_events
    SET from_status = 'CHECKED_IN'::booking_status
    WHERE from_status = 'IN_PROGRESS'::booking_status;

    UPDATE booking_status_events
    SET to_status = 'CHECKED_IN'::booking_status
    WHERE to_status = 'IN_PROGRESS'::booking_status;

    ALTER TABLE bookings ALTER COLUMN status DROP DEFAULT;

    ALTER TYPE booking_status RENAME TO booking_status_old;

    CREATE TYPE booking_status AS ENUM (
      'DRAFT',
      'CONFIRMED',
      'CHECKED_IN',
      'COMPLETED',
      'CANCELLED',
      'NO_SHOW'
    );

    ALTER TABLE bookings
      ALTER COLUMN status TYPE booking_status
      USING status::text::booking_status;

    ALTER TABLE bookings
      ALTER COLUMN status SET DEFAULT 'DRAFT'::booking_status;

    ALTER TABLE booking_status_events
      ALTER COLUMN from_status TYPE booking_status
      USING from_status::text::booking_status;

    ALTER TABLE booking_status_events
      ALTER COLUMN to_status TYPE booking_status
      USING to_status::text::booking_status;

    DROP TYPE booking_status_old;
  END IF;
END $$;
