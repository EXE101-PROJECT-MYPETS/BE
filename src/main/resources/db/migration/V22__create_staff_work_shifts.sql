CREATE TABLE IF NOT EXISTS staff_work_shifts (
    id bigserial PRIMARY KEY,
    shop_id bigint NOT NULL REFERENCES shops(id) ON DELETE CASCADE,
    user_id bigint NOT NULL,
    work_date date NOT NULL,
    start_time time NOT NULL,
    end_time time NOT NULL,
    note text,
    created_by bigint REFERENCES users(id) ON DELETE SET NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT fk_staff_work_shifts_member_shop
        FOREIGN KEY (shop_id, user_id)
        REFERENCES shop_members(shop_id, user_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_staff_work_shifts_time_range
        CHECK (end_time > start_time),

    CONSTRAINT uq_staff_work_shifts_exact
        UNIQUE (shop_id, user_id, work_date, start_time, end_time)
);

CREATE INDEX IF NOT EXISTS idx_staff_work_shifts_shop_date
    ON staff_work_shifts(shop_id, work_date);

CREATE INDEX IF NOT EXISTS idx_staff_work_shifts_shop_user_date
    ON staff_work_shifts(shop_id, user_id, work_date);

DROP TRIGGER IF EXISTS trg_staff_work_shifts_updated_at ON staff_work_shifts;

CREATE TRIGGER trg_staff_work_shifts_updated_at
    BEFORE UPDATE ON staff_work_shifts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
