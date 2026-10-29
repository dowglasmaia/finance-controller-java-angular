-- Tokens de recuperação de senha: só o hash SHA-256 é guardado; uso único; validade curta.
CREATE TABLE password_reset_token (
    id         uuid         NOT NULL,
    user_id    uuid         NOT NULL,
    token_hash varchar(64)  NOT NULL,
    expires_at timestamptz  NOT NULL,
    used_at    timestamptz,
    created_at timestamptz  NOT NULL,
    CONSTRAINT pk_password_reset_token PRIMARY KEY (id),
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT uq_password_reset_token_hash UNIQUE (token_hash)
);

-- invalidar os tokens de uma conta (novo pedido, senha redefinida) e a exclusão em cascata do utilizador
CREATE INDEX ix_password_reset_token_user ON password_reset_token (user_id);
-- limpeza periódica dos vencidos
CREATE INDEX ix_password_reset_token_expires ON password_reset_token (expires_at);
