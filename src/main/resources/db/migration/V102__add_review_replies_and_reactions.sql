-- Add reply columns to product reviews
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reply text;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS reply_at timestamptz;

-- Add reply columns to service reviews
ALTER TABLE service_reviews ADD COLUMN IF NOT EXISTS reply text;
ALTER TABLE service_reviews ADD COLUMN IF NOT EXISTS reply_at timestamptz;

-- Create product review reactions table
CREATE TABLE IF NOT EXISTS review_reactions (
    id bigserial PRIMARY KEY,
    review_id bigint NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_like boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_review_reactions_review_user UNIQUE (review_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_review_reactions_review_id ON review_reactions(review_id);
CREATE INDEX IF NOT EXISTS idx_review_reactions_user_id ON review_reactions(user_id);

-- Create service review reactions table
CREATE TABLE IF NOT EXISTS service_review_reactions (
    id bigserial PRIMARY KEY,
    service_review_id bigint NOT NULL REFERENCES service_reviews(id) ON DELETE CASCADE,
    user_id bigint NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_like boolean NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_service_review_reactions_review_user UNIQUE (service_review_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_service_review_reactions_review_id ON service_review_reactions(service_review_id);
CREATE INDEX IF NOT EXISTS idx_service_review_reactions_user_id ON service_review_reactions(service_review_id);
