CREATE TABLE IF NOT EXISTS shop_ghtk_configs (
    id bigserial PRIMARY KEY,

    shop_id bigint NOT NULL UNIQUE REFERENCES shops(id) ON DELETE CASCADE,

    enabled boolean NOT NULL DEFAULT false,

    encrypted_api_token text NOT NULL,
    client_source varchar(100),

    pick_name varchar(255) NOT NULL,
    pick_tel varchar(20) NOT NULL,
    pick_address text NOT NULL,
    pick_province varchar(100) NOT NULL,
    pick_district varchar(100) NOT NULL,
    pick_ward varchar(100),

    pick_option varchar(20) NOT NULL DEFAULT 'cod',
    transport varchar(20) NOT NULL DEFAULT 'road',

    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),

    CONSTRAINT chk_shop_ghtk_configs_api_token_not_blank
        CHECK (length(btrim(encrypted_api_token)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_client_source_not_blank
        CHECK (client_source IS NULL OR length(btrim(client_source)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_pick_name_not_blank
        CHECK (length(btrim(pick_name)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_pick_tel_not_blank
        CHECK (length(btrim(pick_tel)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_pick_address_not_blank
        CHECK (length(btrim(pick_address)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_pick_province_not_blank
        CHECK (length(btrim(pick_province)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_pick_district_not_blank
        CHECK (length(btrim(pick_district)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_pick_ward_not_blank
        CHECK (pick_ward IS NULL OR length(btrim(pick_ward)) > 0),
    CONSTRAINT chk_shop_ghtk_configs_pick_option
        CHECK (pick_option IN ('cod', 'post')),
    CONSTRAINT chk_shop_ghtk_configs_transport
        CHECK (transport IN ('road', 'fly'))
);

DROP TRIGGER IF EXISTS trg_shop_ghtk_configs_updated_at ON shop_ghtk_configs;

CREATE TRIGGER trg_shop_ghtk_configs_updated_at
    BEFORE UPDATE ON shop_ghtk_configs
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
