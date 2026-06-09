CREATE SCHEMA IF NOT EXISTS prod;

DO
$$
BEGIN
  IF
NOT EXISTS (SELECT 1 FROM pg_type WHERE typnamespace = 'prod'::regnamespace AND typname = 'shipment_status') THEN
CREATE TYPE prod.shipment_status AS ENUM (
      'CREATED',
      'PENDING_PICKUP',
      'ACCEPTED',
      'PICKED_UP',
      'DELIVERING',
      'DELIVERED',
      'RECONCILED',
      'CANCELED',
      'PICKUP_FAILED',
      'PICKUP_DELAYED',
      'DELIVERY_FAILED',
      'DELIVERY_DELAYED',
      'RETURNING',
      'RETURNED'
    );
END IF;

  IF
NOT EXISTS (
    SELECT 1
    FROM pg_type
    WHERE typnamespace = 'prod'::regnamespace
      AND typname = 'shipping_webhook_processing_status'
  ) THEN
CREATE TYPE prod.shipping_webhook_processing_status AS ENUM (
      'RECEIVED',
      'UNKNOWN_SHIPMENT',
      'STALE_OR_DUPLICATE',
      'SHIPPER_EVENT',
      'APPLIED',
      'FAILED'
    );
END IF;
END $$;

CREATE TABLE IF NOT EXISTS prod.shop_order_shipments
(
    id
    bigserial
    PRIMARY
    KEY,
    shop_id
    bigint
    NOT
    NULL
    REFERENCES
    prod
    .
    shops
(
    id
) ON DELETE CASCADE,
    order_id bigint NOT NULL,
    carrier varchar
(
    32
) NOT NULL DEFAULT 'GHTK',

    partner_id varchar
(
    100
) NOT NULL,
    label_id varchar
(
    100
),
    tracking_id varchar
(
    100
),

    status prod.shipment_status NOT NULL DEFAULT 'CREATED',
    ghtk_status_id integer,
    last_action_time timestamptz,

    actual_shipping_fee bigint CHECK
(
    actual_shipping_fee
    IS
    NULL
    OR
    actual_shipping_fee
    >=
    0
),
    weight numeric
(
    10,
    3
) CHECK
(
    weight
    IS
    NULL
    OR
    weight
    >=
    0
),
    pick_money bigint CHECK
(
    pick_money
    IS
    NULL
    OR
    pick_money
    >=
    0
),
    return_part_package integer,
    reason_code varchar
(
    50
),
    reason text,

    created_at timestamptz NOT NULL DEFAULT now
(
),
    updated_at timestamptz NOT NULL DEFAULT now
(
),
    CONSTRAINT fk_shop_order_shipments_order_shop
    FOREIGN KEY
(
    shop_id,
    order_id
)
    REFERENCES prod.orders
(
    shop_id,
    id
)
  ON DELETE CASCADE,
    CONSTRAINT uq_shop_order_shipments_order UNIQUE
(
    order_id
),
    CONSTRAINT uq_shop_order_shipments_partner UNIQUE
(
    partner_id
),
    CONSTRAINT uq_shop_order_shipments_label UNIQUE
(
    label_id
)
    );

DO
$$
BEGIN
  IF
NOT EXISTS (SELECT 1 FROM pg_trigger WHERE tgname = 'trg_shop_order_shipments_updated_at') THEN
CREATE TRIGGER trg_shop_order_shipments_updated_at
    BEFORE UPDATE
    ON prod.shop_order_shipments
    FOR EACH ROW EXECUTE FUNCTION prod.set_updated_at();
END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_shop_order_shipments_shop_order
    ON prod.shop_order_shipments(shop_id, order_id);

CREATE INDEX IF NOT EXISTS idx_shop_order_shipments_status
    ON prod.shop_order_shipments(status);

CREATE TABLE IF NOT EXISTS prod.shipping_webhook_logs
(
    id
    bigserial
    PRIMARY
    KEY,
    carrier
    varchar
(
    32
) NOT NULL DEFAULT 'GHTK',
    shipment_id bigint REFERENCES prod.shop_order_shipments
(
    id
) ON DELETE SET NULL,
    shop_id bigint REFERENCES prod.shops
(
    id
)
  ON DELETE SET NULL,
    order_id bigint REFERENCES prod.orders
(
    id
)
  ON DELETE SET NULL,

    partner_id varchar
(
    100
),
    label_id varchar
(
    100
),
    status_id integer,
    action_time timestamptz,
    hash varchar
(
    255
),
    raw_payload_json jsonb NOT NULL DEFAULT '{}'::jsonb,
    processing_status prod.shipping_webhook_processing_status NOT NULL,
    error_message text,
    created_at timestamptz NOT NULL DEFAULT now
(
)
    );

CREATE INDEX IF NOT EXISTS idx_shipping_webhook_logs_shipment_created
    ON prod.shipping_webhook_logs(shipment_id, created_at);

CREATE INDEX IF NOT EXISTS idx_shipping_webhook_logs_order_created
    ON prod.shipping_webhook_logs(order_id, created_at);

CREATE INDEX IF NOT EXISTS idx_shipping_webhook_logs_partner_created
    ON prod.shipping_webhook_logs(partner_id, created_at);

CREATE INDEX IF NOT EXISTS idx_shipping_webhook_logs_label_created
    ON prod.shipping_webhook_logs(label_id, created_at);
