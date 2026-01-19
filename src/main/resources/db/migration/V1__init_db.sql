-- =========================================================
-- Pet Spa Database - PostgreSQL DDL
-- =========================================================
-- Notes:
-- - Uses PostGIS for shops.location
-- - Uses ENUM types for status/roles/types
-- - Uses composite FK (shop_id, id) to prevent cross-shop references
-- - Uses partial unique indexes for nullable phone/email fields
-- - Uses triggers to maintain updated_at
-- =========================================================
BEGIN;

-- -----------------------------
-- Extensions
-- -----------------------------
-- -----------------------------
-- ENUM types
-- -----------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'shop_status') THEN
CREATE TYPE shop_status AS ENUM ('ACTIVE', 'INACTIVE', 'SUSPENDED');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'user_status') THEN
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'BLOCKED');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'member_status') THEN
CREATE TYPE member_status AS ENUM ('ACTIVE', 'INACTIVE', 'INVITED', 'REMOVED');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'shop_role') THEN
CREATE TYPE shop_role AS ENUM ('OWNER', 'MANAGER', 'STAFF');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'location_source') THEN
CREATE TYPE location_source AS ENUM ('MANUAL', 'BROWSER_GEO', 'PLACE_PICKER');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'resource_type') THEN
CREATE TYPE resource_type AS ENUM ('STAFF', 'ROOM', 'TUB', 'EQUIPMENT');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_status') THEN
CREATE TYPE booking_status AS ENUM (
      'DRAFT', 'CONFIRMED', 'CHECKED_IN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'
    );
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_source') THEN
CREATE TYPE booking_source AS ENUM ('STAFF', 'CUSTOMER', 'SYSTEM');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'booking_item_type') THEN
CREATE TYPE booking_item_type AS ENUM ('SERVICE', 'PRODUCT', 'PACKAGE_REDEEM', 'ADJUSTMENT');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'inventory_reason') THEN
CREATE TYPE inventory_reason AS ENUM ('SALE', 'ADJUST', 'RETURN', 'INIT', 'PURCHASE');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'package_ledger_reason') THEN
CREATE TYPE package_ledger_reason AS ENUM ('PURCHASE', 'REDEEM', 'REFUND', 'ADJUST', 'CANCEL');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'invoice_status') THEN
CREATE TYPE invoice_status AS ENUM ('DRAFT', 'ISSUED', 'PAID', 'VOID', 'CANCELLED');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_provider') THEN
CREATE TYPE payment_provider AS ENUM ('VNPAY', 'MOMO', 'STRIPE', 'CASH', 'BANK_TRANSFER');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'payment_intent_status') THEN
CREATE TYPE payment_intent_status AS ENUM (
      'REQUIRES_PAYMENT_METHOD', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED'
    );
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'conversation_member_type') THEN
CREATE TYPE conversation_member_type AS ENUM ('CUSTOMER', 'STAFF');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'message_sender_type') THEN
CREATE TYPE message_sender_type AS ENUM ('CUSTOMER', 'STAFF');
END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'credential_provider') THEN
CREATE TYPE credential_provider AS ENUM ('LOCAL', 'GOOGLE', 'APPLE');
END IF;
END $$;

-- -----------------------------
-- updated_at trigger helper
-- -----------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- =========================================================
-- Core: shops, users, membership
-- =========================================================

CREATE TABLE IF NOT EXISTS shops (
                                     id bigserial PRIMARY KEY,
                                     name text NOT NULL,

                                     address_text text,

                                     lat double precision NOT NULL,
                                     lng double precision NOT NULL,
                                     location_source location_source NOT NULL DEFAULT 'MANUAL',
                                     location_accuracy_m int,
                                     location_updated_at timestamptz NOT NULL DEFAULT now(),

    status shop_status NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CHECK (lat BETWEEN -90 AND 90),
    CHECK (lng BETWEEN -180 AND 180)
    );

-- Indexes for shops
CREATE INDEX IF NOT EXISTS idx_shops_status ON shops(status);

-- thay GIST(location) bằng index thường cho lat/lng
CREATE INDEX IF NOT EXISTS idx_shops_lat_lng ON shops(lat, lng);

CREATE TRIGGER trg_shops_updated_at
    BEFORE UPDATE ON shops
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
                                     id bigserial PRIMARY KEY,
                                     email varchar(255) UNIQUE,
    phone varchar(50) UNIQUE,
    full_name varchar(255),
    status user_status NOT NULL DEFAULT 'ACTIVE',

    address varchar(255),
    age int CHECK (age IS NULL OR (age >= 0 AND age <= 150)),
    avatar_url_preview varchar(1000),

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
    );

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS shop_members (
                                            shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    role shop_role NOT NULL,
    status member_status NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (shop_id, user_id)
    );

CREATE INDEX IF NOT EXISTS idx_shop_members_user_id ON shop_members(user_id);

-- =========================================================
-- Customers & Pets (multi-tenant safe)
-- =========================================================

CREATE TABLE IF NOT EXISTS customers (
                                         id bigserial PRIMARY KEY,
                                         shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id bigint REFERENCES users(id) ON DELETE SET NULL,

    full_name varchar(255) NOT NULL,
    phone varchar(50),
    email varchar(255),

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    -- Used to support composite FK from child tables
    UNIQUE (shop_id, id)
    );

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- IMPORTANT: phone/email are nullable -> use partial unique indexes
CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_shop_phone_notnull
    ON customers(shop_id, phone)
    WHERE phone IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_shop_email_notnull
    ON customers(shop_id, email)
    WHERE email IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_customers_shop_created_at ON customers(shop_id, created_at);

-- ---------------------------------------------------------
-- Species & Breeds (global catalog)
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS pet_species (
                                           id bigserial PRIMARY KEY,
                                           name varchar(50) NOT NULL UNIQUE
    );

CREATE TABLE IF NOT EXISTS pet_breeds (
                                          id bigserial PRIMARY KEY,
                                          species_id bigint NOT NULL REFERENCES pet_species(id) ON DELETE RESTRICT,
    name varchar(100) NOT NULL,

    UNIQUE (species_id, name)
    );

CREATE INDEX IF NOT EXISTS idx_pet_breeds_species_id ON pet_breeds(species_id);

-- ---------------------------------------------------------
-- Pets (multi-tenant safe + species/breed)
-- ---------------------------------------------------------

CREATE TABLE IF NOT EXISTS pets (
                                    id bigserial PRIMARY KEY,
                                    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    -- Composite FK prevents cross-shop customer reference
    customer_id bigint NOT NULL,
    CONSTRAINT fk_pets_customer_shop
    FOREIGN KEY (shop_id, customer_id)
    REFERENCES customers(shop_id, id)
                                                                                 ON DELETE CASCADE,

    species_id bigint NOT NULL REFERENCES pet_species(id) ON DELETE RESTRICT,
    breed_id bigint REFERENCES pet_breeds(id) ON DELETE SET NULL,
    breed_text varchar(100),

    avatar_url text,
    name varchar(255) NOT NULL,
    gender varchar(20),
    dob date,
    note text,

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    -- Help avoid nonsense dob
    CHECK (dob IS NULL OR dob <= current_date),

    -- Optional: enforce that either breed_id or breed_text can be present (not required)
    CHECK (
              breed_id IS NULL OR breed_text IS NULL OR length(breed_text) >= 0
    ),

    UNIQUE (shop_id, id)
    );

CREATE TRIGGER trg_pets_updated_at
    BEFORE UPDATE ON pets
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_pets_shop_customer ON pets(shop_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_pets_species ON pets(species_id);
CREATE INDEX IF NOT EXISTS idx_pets_breed ON pets(breed_id);

-- =========================================================
-- Pet health + Vaccines
-- =========================================================

CREATE TABLE IF NOT EXISTS pet_health_profiles (
                                                   pet_id bigint PRIMARY KEY REFERENCES pets(id) ON DELETE CASCADE,
    allergies text,
    conditions text,
    notes text,
    updated_at timestamptz NOT NULL DEFAULT now()
    );

-- Vaccines catalog (global) tied to species
CREATE TABLE IF NOT EXISTS vaccines (
                                        id bigserial PRIMARY KEY,
                                        species_id bigint NOT NULL REFERENCES pet_species(id) ON DELETE RESTRICT,
    name varchar(100) NOT NULL,
    description text,
    booster_interval_days int CHECK (booster_interval_days IS NULL OR booster_interval_days >= 0),
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    UNIQUE (species_id, name)
    );

CREATE TRIGGER trg_vaccines_updated_at
    BEFORE UPDATE ON vaccines
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_vaccines_species_id ON vaccines(species_id);

-- Pet vaccination events (multi-tenant safe)
CREATE TABLE IF NOT EXISTS pet_vaccinations (
                                                id bigserial PRIMARY KEY,
                                                shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    pet_id bigint NOT NULL,
    CONSTRAINT fk_pet_vaccinations_pet_shop
    FOREIGN KEY (shop_id, pet_id)
    REFERENCES pets(shop_id, id)
                                                                                             ON DELETE CASCADE,

    vaccine_id bigint NOT NULL REFERENCES vaccines(id) ON DELETE RESTRICT,

    vaccinated_at date NOT NULL,
    next_due_at date,
    clinic_name varchar(255),
    vet_name varchar(255),
    batch_no varchar(100),
    note text,

    created_by bigint REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    CHECK (next_due_at IS NULL OR next_due_at >= vaccinated_at)
    );

CREATE INDEX IF NOT EXISTS idx_pet_vaccinations_shop_pet_date
    ON pet_vaccinations(shop_id, pet_id, vaccinated_at);

CREATE INDEX IF NOT EXISTS idx_pet_vaccinations_pet_id ON pet_vaccinations(pet_id);
CREATE INDEX IF NOT EXISTS idx_pet_vaccinations_vaccine_id ON pet_vaccinations(vaccine_id);

-- =========================================================
-- Services, Products, Inventory
-- =========================================================

CREATE TABLE IF NOT EXISTS services (
                                        id bigserial PRIMARY KEY,
                                        shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    duration_min int NOT NULL CHECK (duration_min > 0),
    base_price bigint NOT NULL CHECK (base_price >= 0),
    active boolean NOT NULL DEFAULT true,

    UNIQUE (shop_id, name),
    UNIQUE (shop_id, id)
    );

CREATE INDEX IF NOT EXISTS idx_services_shop_active ON services(shop_id, active);

CREATE TABLE IF NOT EXISTS products (
                                        id bigserial PRIMARY KEY,
                                        shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    sku varchar(64) NOT NULL,
    name varchar(255) NOT NULL,
    unit varchar(50),
    price bigint NOT NULL DEFAULT 0 CHECK (price >= 0),
    active boolean NOT NULL DEFAULT true,

    UNIQUE (shop_id, sku),
    UNIQUE (shop_id, id)
    );

CREATE INDEX IF NOT EXISTS idx_products_shop_active ON products(shop_id, active);

CREATE TABLE IF NOT EXISTS inventory (
                                         shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    product_id bigint NOT NULL,
    on_hand bigint NOT NULL DEFAULT 0 CHECK (on_hand >= 0),
    reserved bigint NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    updated_at timestamptz NOT NULL DEFAULT now(),

    PRIMARY KEY (shop_id, product_id),

    CONSTRAINT fk_inventory_product_shop
    FOREIGN KEY (shop_id, product_id)
    REFERENCES products(shop_id, id)
                                                                                      ON DELETE CASCADE,

    CHECK (reserved <= on_hand)
    );

CREATE TABLE IF NOT EXISTS inventory_movements (
                                                   id bigserial PRIMARY KEY,
                                                   shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    product_id bigint NOT NULL,
    CONSTRAINT fk_inventory_movements_product_shop
    FOREIGN KEY (shop_id, product_id)
    REFERENCES products(shop_id, id)
                                                                                                ON DELETE RESTRICT,

    qty_delta bigint NOT NULL,
    reason inventory_reason NOT NULL,
    ref_type varchar(20),
    ref_id bigint,

    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_inventory_movements_shop_product_created
    ON inventory_movements(shop_id, product_id, created_at);

-- =========================================================
-- Bookings (cart/appointment core)
-- =========================================================

CREATE TABLE IF NOT EXISTS bookings (
                                        id bigserial PRIMARY KEY,
                                        shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    customer_id bigint NOT NULL,
    CONSTRAINT fk_bookings_customer_shop
    FOREIGN KEY (shop_id, customer_id)
    REFERENCES customers(shop_id, id)
                                                                                     ON DELETE RESTRICT,

    start_at timestamptz NOT NULL,
    end_at timestamptz NOT NULL,
    status booking_status NOT NULL DEFAULT 'DRAFT',
    source booking_source NOT NULL DEFAULT 'STAFF',
    note text,

    created_by bigint REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CHECK (end_at > start_at),

    UNIQUE (shop_id, id)
    );

CREATE TRIGGER trg_bookings_updated_at
    BEFORE UPDATE ON bookings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_bookings_shop_start ON bookings(shop_id, start_at);
CREATE INDEX IF NOT EXISTS idx_bookings_shop_customer_start ON bookings(shop_id, customer_id, start_at);

-- booking_pets (many-to-many)
CREATE TABLE IF NOT EXISTS booking_pets (
                                            booking_id bigint NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    pet_id bigint NOT NULL REFERENCES pets(id) ON DELETE RESTRICT,
    PRIMARY KEY (booking_id, pet_id)
    );

CREATE INDEX IF NOT EXISTS idx_booking_pets_pet_id ON booking_pets(pet_id);

-- booking_items (polymorphic ref_id by item_type)
CREATE TABLE IF NOT EXISTS booking_items (
                                             id bigserial PRIMARY KEY,
                                             shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    booking_id bigint NOT NULL,
    CONSTRAINT fk_booking_items_booking_shop
    FOREIGN KEY (shop_id, booking_id)
    REFERENCES bookings(shop_id, id)
                                                                                          ON DELETE CASCADE,

    pet_id bigint REFERENCES pets(id) ON DELETE SET NULL,

    item_type booking_item_type NOT NULL,
    ref_id bigint, -- cannot FK due to polymorphism

    qty int NOT NULL DEFAULT 1 CHECK (qty > 0),
    unit_price bigint NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    amount bigint NOT NULL DEFAULT 0 CHECK (amount >= 0),

    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_booking_items_booking_id ON booking_items(booking_id);
CREATE INDEX IF NOT EXISTS idx_booking_items_shop_booking ON booking_items(shop_id, booking_id);

-- booking_status_events (audit state changes)
CREATE TABLE IF NOT EXISTS booking_status_events (
                                                     id bigserial PRIMARY KEY,
                                                     shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    booking_id bigint NOT NULL,
    CONSTRAINT fk_booking_status_events_booking_shop
    FOREIGN KEY (shop_id, booking_id)
    REFERENCES bookings(shop_id, id)
                                                                                                  ON DELETE CASCADE,

    from_status booking_status,
    to_status booking_status NOT NULL,

    actor_user_id bigint REFERENCES users(id) ON DELETE SET NULL,
    meta_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_booking_status_events_booking_created
    ON booking_status_events(booking_id, created_at);

-- =========================================================
-- Resources & booking_resources
-- =========================================================

CREATE TABLE IF NOT EXISTS resources (
                                         id bigserial PRIMARY KEY,
                                         shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    type resource_type NOT NULL,
    name varchar(255) NOT NULL,
    active boolean NOT NULL DEFAULT true,

    UNIQUE (shop_id, type, name),
    UNIQUE (shop_id, id)
    );

CREATE TABLE IF NOT EXISTS booking_resources (
                                                 booking_id bigint NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
    resource_id bigint NOT NULL REFERENCES resources(id) ON DELETE RESTRICT,
    PRIMARY KEY (booking_id, resource_id)
    );

-- =========================================================
-- Packages
-- =========================================================

CREATE TABLE IF NOT EXISTS packages (
                                        id bigserial PRIMARY KEY,
                                        shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    name varchar(255) NOT NULL,
    price bigint NOT NULL CHECK (price >= 0),
    total_uses int NOT NULL CHECK (total_uses > 0),
    expiry_days int CHECK (expiry_days IS NULL OR expiry_days > 0),
    active boolean NOT NULL DEFAULT true,

    UNIQUE (shop_id, name),
    UNIQUE (shop_id, id)
    );

CREATE TABLE IF NOT EXISTS customer_packages (
                                                 id bigserial PRIMARY KEY,
                                                 shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    customer_id bigint NOT NULL,
    CONSTRAINT fk_customer_packages_customer_shop
    FOREIGN KEY (shop_id, customer_id)
    REFERENCES customers(shop_id, id)
                                                                                              ON DELETE RESTRICT,

    package_id bigint NOT NULL,
    CONSTRAINT fk_customer_packages_package_shop
    FOREIGN KEY (shop_id, package_id)
    REFERENCES packages(shop_id, id)
                                                                                              ON DELETE RESTRICT,

    purchased_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz,
    status varchar(20) NOT NULL DEFAULT 'ACTIVE',
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_customer_packages_shop_customer_purchased
    ON customer_packages(shop_id, customer_id, purchased_at);

CREATE TABLE IF NOT EXISTS package_ledger (
                                              id bigserial PRIMARY KEY,
                                              shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    customer_package_id bigint NOT NULL REFERENCES customer_packages(id) ON DELETE CASCADE,
    booking_id bigint REFERENCES bookings(id) ON DELETE SET NULL,

    delta_uses int NOT NULL,
    delta_amount bigint NOT NULL DEFAULT 0,
    reason package_ledger_reason NOT NULL,

    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_package_ledger_cp_created
    ON package_ledger(customer_package_id, created_at);

CREATE INDEX IF NOT EXISTS idx_package_ledger_cp_booking_reason
    ON package_ledger(customer_package_id, booking_id, reason);

-- =========================================================
-- Invoices & Payments
-- =========================================================

CREATE TABLE IF NOT EXISTS invoices (
                                        id bigserial PRIMARY KEY,
                                        shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    customer_id bigint NOT NULL,
    CONSTRAINT fk_invoices_customer_shop
    FOREIGN KEY (shop_id, customer_id)
    REFERENCES customers(shop_id, id)
                                                                                     ON DELETE RESTRICT,

    booking_id bigint,
    CONSTRAINT fk_invoices_booking_shop
    FOREIGN KEY (shop_id, booking_id)
    REFERENCES bookings(shop_id, id)
                                                                                     ON DELETE SET NULL,

    total_amount bigint NOT NULL DEFAULT 0 CHECK (total_amount >= 0),
    status invoice_status NOT NULL DEFAULT 'DRAFT',
    issued_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    UNIQUE (shop_id, id)
    );

CREATE TRIGGER trg_invoices_updated_at
    BEFORE UPDATE ON invoices
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE INDEX IF NOT EXISTS idx_invoices_shop_created ON invoices(shop_id, created_at);
CREATE INDEX IF NOT EXISTS idx_invoices_shop_booking ON invoices(shop_id, booking_id);

CREATE TABLE IF NOT EXISTS invoice_lines (
                                             id bigserial PRIMARY KEY,
                                             shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    invoice_id bigint NOT NULL,
    CONSTRAINT fk_invoice_lines_invoice_shop
    FOREIGN KEY (shop_id, invoice_id)
    REFERENCES invoices(shop_id, id)
                                                                                          ON DELETE CASCADE,

    line_type varchar(20) NOT NULL, -- keep as text for flexibility; can enum later
    ref_id bigint,
    qty int NOT NULL DEFAULT 1 CHECK (qty > 0),
    unit_price bigint NOT NULL DEFAULT 0 CHECK (unit_price >= 0),
    amount bigint NOT NULL DEFAULT 0 CHECK (amount >= 0)
    );

CREATE INDEX IF NOT EXISTS idx_invoice_lines_invoice_id ON invoice_lines(invoice_id);

-- Payments
CREATE TABLE IF NOT EXISTS payment_intents (
                                               id bigserial PRIMARY KEY,
                                               shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    invoice_id bigint NOT NULL,
    CONSTRAINT fk_payment_intents_invoice_shop
    FOREIGN KEY (shop_id, invoice_id)
    REFERENCES invoices(shop_id, id)
                                                                                            ON DELETE CASCADE,

    provider payment_provider NOT NULL,
    method varchar(30) NOT NULL DEFAULT 'UNKNOWN',
    amount bigint NOT NULL CHECK (amount > 0),
    currency varchar(10) NOT NULL DEFAULT 'VND',
    status payment_intent_status NOT NULL DEFAULT 'REQUIRES_PAYMENT_METHOD',
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE INDEX IF NOT EXISTS idx_payment_intents_invoice_id ON payment_intents(invoice_id);

CREATE TABLE IF NOT EXISTS payment_transactions (
                                                    id bigserial PRIMARY KEY,
                                                    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    payment_intent_id bigint NOT NULL REFERENCES payment_intents(id) ON DELETE CASCADE,
    provider_txn_id varchar(255) NOT NULL,
    status varchar(40) NOT NULL,
    raw_payload_json jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_payment_txn_shop_provider_txn
    ON payment_transactions(shop_id, provider_txn_id);

CREATE INDEX IF NOT EXISTS idx_payment_txn_intent_created
    ON payment_transactions(payment_intent_id, created_at);

-- =========================================================
-- Auth: credentials & refresh tokens
-- =========================================================

CREATE TABLE IF NOT EXISTS user_credentials (
                                                user_id bigint PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    provider credential_provider NOT NULL DEFAULT 'LOCAL',
    password_hash text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    -- If provider is LOCAL, password_hash should exist
    CHECK (provider <> 'LOCAL' OR password_hash IS NOT NULL)
    );

CREATE TRIGGER trg_user_credentials_updated_at
    BEFORE UPDATE ON user_credentials
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS refresh_tokens (
                                              id bigserial PRIMARY KEY,
                                              user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash text NOT NULL UNIQUE,
    device_id varchar(100),
    user_agent text,
    ip_address varchar(45),
    created_at timestamptz NOT NULL DEFAULT now(),
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,

    CHECK (expires_at > created_at)
    );

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_revoked_expires ON refresh_tokens(user_id, revoked_at, expires_at);

-- =========================================================
-- Conversations & Messages (FIXED: no nullable columns in PK)
-- =========================================================

CREATE TABLE IF NOT EXISTS conversations (
                                             id bigserial PRIMARY KEY,
                                             shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    customer_id bigint NOT NULL,
    CONSTRAINT fk_conversations_customer_shop
    FOREIGN KEY (shop_id, customer_id)
    REFERENCES customers(shop_id, id)
                                                                                          ON DELETE CASCADE,

    created_at timestamptz NOT NULL DEFAULT now(),

    UNIQUE (shop_id, customer_id),
    UNIQUE (shop_id, id)
    );

CREATE INDEX IF NOT EXISTS idx_conversations_shop_created ON conversations(shop_id, created_at);

-- FIX: Use surrogate PK, plus checks + partial unique indexes
CREATE TABLE IF NOT EXISTS conversation_members (
                                                    id bigserial PRIMARY KEY,
                                                    conversation_id bigint NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    member_type conversation_member_type NOT NULL,

    customer_id bigint REFERENCES customers(id) ON DELETE CASCADE,
    user_id bigint REFERENCES users(id) ON DELETE CASCADE,

    last_read_message_id bigint,
    created_at timestamptz NOT NULL DEFAULT now(),

    CHECK (
(member_type = 'CUSTOMER' AND customer_id IS NOT NULL AND user_id IS NULL)
    OR
(member_type = 'STAFF' AND user_id IS NOT NULL AND customer_id IS NULL)
    )
    );

-- One CUSTOMER member per conversation for that customer
CREATE UNIQUE INDEX IF NOT EXISTS uq_conv_members_customer
    ON conversation_members(conversation_id, customer_id)
    WHERE member_type = 'CUSTOMER';

-- One STAFF member per conversation for that staff user
CREATE UNIQUE INDEX IF NOT EXISTS uq_conv_members_staff
    ON conversation_members(conversation_id, user_id)
    WHERE member_type = 'STAFF';

CREATE INDEX IF NOT EXISTS idx_conv_members_user_id ON conversation_members(user_id);
CREATE INDEX IF NOT EXISTS idx_conv_members_customer_id ON conversation_members(customer_id);

-- Messages
CREATE TABLE IF NOT EXISTS messages (
                                        id bigserial PRIMARY KEY,
                                        conversation_id bigint NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,

    sender_type message_sender_type NOT NULL,
    sender_customer_id bigint REFERENCES customers(id) ON DELETE SET NULL,
    sender_user_id bigint REFERENCES users(id) ON DELETE SET NULL,

    body text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),

    CHECK (
(sender_type = 'CUSTOMER' AND sender_customer_id IS NOT NULL AND sender_user_id IS NULL)
    OR
(sender_type = 'STAFF' AND sender_user_id IS NOT NULL AND sender_customer_id IS NULL)
    )
    );

CREATE INDEX IF NOT EXISTS idx_messages_conversation_id_id ON messages(conversation_id, id);
CREATE INDEX IF NOT EXISTS idx_messages_shop_created ON messages(shop_id, created_at);

COMMIT;
