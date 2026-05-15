CREATE TABLE IF NOT EXISTS search_histories (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    keyword varchar(255) NOT NULL,
    search_count int NOT NULL DEFAULT 1,
    last_searched_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_search_histories_user_keyword
    ON search_histories(user_id, keyword);

CREATE INDEX IF NOT EXISTS idx_search_histories_user_last_searched_at
    ON search_histories(user_id, last_searched_at DESC, id DESC);

CREATE INDEX IF NOT EXISTS idx_search_histories_keyword_search_count
    ON search_histories(lower(keyword), search_count DESC, last_searched_at DESC);
