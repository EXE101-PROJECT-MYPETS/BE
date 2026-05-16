ALTER TABLE prod.ai_knowledge_embeddings
    ADD COLUMN IF NOT EXISTS search_text tsvector;

UPDATE prod.ai_knowledge_embeddings
SET search_text =
        to_tsvector(
                'simple',
                unaccent(
                        coalesce(title, '') || ' ' ||
                        coalesce(content, '') || ' ' ||
                        coalesce(topic, '')
                )
        );

CREATE INDEX IF NOT EXISTS idx_ai_knowledge_embeddings_search_text
    ON prod.ai_knowledge_embeddings
        USING gin (search_text);

CREATE OR REPLACE FUNCTION prod.update_ai_knowledge_search_text()
RETURNS trigger AS $$
BEGIN
    NEW.search_text :=
            to_tsvector(
                    'simple',
                    unaccent(
                            coalesce(NEW.title, '') || ' ' ||
                            coalesce(NEW.content, '') || ' ' ||
                            coalesce(NEW.topic, '')
                    )
            );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ai_knowledge_search_text
    ON prod.ai_knowledge_embeddings;

CREATE TRIGGER trg_ai_knowledge_search_text
    BEFORE INSERT OR UPDATE OF title, content, topic
    ON prod.ai_knowledge_embeddings
    FOR EACH ROW
EXECUTE FUNCTION prod.update_ai_knowledge_search_text();
