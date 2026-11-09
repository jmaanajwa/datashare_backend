CREATE TABLE app_users (
    id UUID PRIMARY KEY,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE stored_files (
    id UUID PRIMARY KEY,
    owner_id UUID REFERENCES app_users(id),
    original_name VARCHAR(255) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    password_hash VARCHAR(255),
    share_token VARCHAR(64) NOT NULL UNIQUE
);

CREATE TABLE file_tags (
    file_id UUID NOT NULL REFERENCES stored_files(id) ON DELETE CASCADE,
    tag VARCHAR(30) NOT NULL,
    PRIMARY KEY (file_id, tag)
);

CREATE INDEX idx_files_owner ON stored_files(owner_id);
CREATE INDEX idx_files_expiration ON stored_files(expires_at);
