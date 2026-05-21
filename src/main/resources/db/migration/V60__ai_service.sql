-- =========================================================
-- V62__recreate_ai_pet_health_tables.sql
-- Recreate AI Pet Health tables
-- Supabase pgvector uses extensions.vector
-- =========================================================

DROP TABLE IF EXISTS prod.ai_pet_chat_messages CASCADE;
DROP TABLE IF EXISTS prod.ai_pet_chat_conversations CASCADE;
DROP TABLE IF EXISTS prod.ai_knowledge_embeddings CASCADE;


-- =========================================================
-- 1. AI Pet Chat Conversations
-- =========================================================

CREATE TABLE prod.ai_pet_chat_conversations (
                                                id BIGSERIAL PRIMARY KEY,

                                                customer_id BIGINT,
                                                user_id BIGINT,
                                                pet_id BIGINT NOT NULL,

                                                title VARCHAR(255),

                                                created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                                updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                                CONSTRAINT fk_ai_pet_chat_conversations_pet
                                                    FOREIGN KEY (pet_id)
                                                        REFERENCES prod.pets(id)
                                                        ON DELETE CASCADE,

                                                CONSTRAINT fk_ai_pet_chat_conversations_customer
                                                    FOREIGN KEY (customer_id)
                                                        REFERENCES prod.customers(id)
                                                        ON DELETE SET NULL,

                                                CONSTRAINT fk_ai_pet_chat_conversations_user
                                                    FOREIGN KEY (user_id)
                                                        REFERENCES prod.users(id)
                                                        ON DELETE SET NULL
);

CREATE INDEX idx_ai_pet_chat_conversations_pet
    ON prod.ai_pet_chat_conversations(pet_id);

CREATE INDEX idx_ai_pet_chat_conversations_customer
    ON prod.ai_pet_chat_conversations(customer_id);

CREATE INDEX idx_ai_pet_chat_conversations_user
    ON prod.ai_pet_chat_conversations(user_id);

CREATE INDEX idx_ai_pet_chat_conversations_updated_at
    ON prod.ai_pet_chat_conversations(updated_at DESC);


-- =========================================================
-- 2. AI Pet Chat Messages
-- =========================================================

CREATE TABLE prod.ai_pet_chat_messages (
                                           id BIGSERIAL PRIMARY KEY,

                                           conversation_id BIGINT NOT NULL,

                                           role VARCHAR(20) NOT NULL,
                                           content TEXT NOT NULL,

                                           metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

                                           created_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                           CONSTRAINT fk_ai_pet_chat_messages_conversation
                                               FOREIGN KEY (conversation_id)
                                                   REFERENCES prod.ai_pet_chat_conversations(id)
                                                   ON DELETE CASCADE,

                                           CONSTRAINT chk_ai_pet_chat_messages_role
                                               CHECK (role IN ('USER', 'ASSISTANT', 'SYSTEM'))
);

CREATE INDEX idx_ai_pet_chat_messages_conversation
    ON prod.ai_pet_chat_messages(conversation_id);

CREATE INDEX idx_ai_pet_chat_messages_created_at
    ON prod.ai_pet_chat_messages(created_at);


-- =========================================================
-- 3. AI Knowledge Embeddings
-- =========================================================

CREATE TABLE prod.ai_knowledge_embeddings (
                                              id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                              topic VARCHAR(100),
                                              source_type VARCHAR(50) NOT NULL,
                                              source_id BIGINT,

                                              title VARCHAR(255),
                                              content TEXT NOT NULL,
                                              metadata JSONB NOT NULL DEFAULT '{}'::jsonb,

                                              embedding prod.vector(1536) NOT NULL,

                                              created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
                                              updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

                                              CONSTRAINT chk_ai_knowledge_embeddings_source_type
                                                  CHECK (source_type IN (
                                                                         'ARTICLE',
                                                                         'FAQ',
                                                                         'DISEASE_GUIDE',
                                                                         'VACCINE_GUIDE',
                                                                         'CARE_GUIDE'
                                                      ))
);

CREATE INDEX idx_ai_knowledge_embeddings_hnsw
    ON prod.ai_knowledge_embeddings
    USING hnsw (embedding prod.vector_cosine_ops);

CREATE INDEX idx_ai_knowledge_embeddings_topic
    ON prod.ai_knowledge_embeddings(topic);

CREATE INDEX idx_ai_knowledge_embeddings_source
    ON prod.ai_knowledge_embeddings(source_type, source_id);

CREATE INDEX idx_ai_knowledge_embeddings_created_at
    ON prod.ai_knowledge_embeddings(created_at DESC);


-- =========================================================
-- 4. updated_at triggers
-- DB của bạn đã có function prod.set_updated_at()
-- =========================================================

CREATE TRIGGER trg_ai_pet_chat_conversations_updated_at
    BEFORE UPDATE ON prod.ai_pet_chat_conversations
    FOR EACH ROW
    EXECUTE FUNCTION prod.set_updated_at();

CREATE TRIGGER trg_ai_knowledge_embeddings_updated_at
    BEFORE UPDATE ON prod.ai_knowledge_embeddings
    FOR EACH ROW
    EXECUTE FUNCTION prod.set_updated_at();