CREATE TABLE IF NOT EXISTS notes_table (
    id SERIAL PRIMARY KEY,
    note TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tags_table (
    id SERIAL PRIMARY KEY,
    tag TEXT UNIQUE NOT NULL CHECK (UPPER(tag) = tag),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS notes_tags_table (
    id SERIAL PRIMARY KEY,
    note_id INTEGER REFERENCES notes_table(id) NOT NULL,
    tag_id INTEGER REFERENCES tags_table(id) NOT NULL,
    UNIQUE(note_id, tag_id)
);