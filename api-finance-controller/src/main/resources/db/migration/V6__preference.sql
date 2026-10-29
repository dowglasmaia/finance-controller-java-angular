-- Preferências por utilizador (moeda e tema). Ausência de linha = padrões (BRL e DARK), sem gravar nada na leitura.
CREATE TABLE preference (
    id         uuid        NOT NULL,
    owner_id   uuid        NOT NULL,
    currency   varchar(3)  NOT NULL,
    theme      varchar(10) NOT NULL,
    updated_at timestamptz NOT NULL,
    CONSTRAINT pk_preference PRIMARY KEY (id),
    CONSTRAINT fk_preference_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    -- um registo por utilizador; também é a chave do INSERT ... ON CONFLICT
    CONSTRAINT uk_preference_owner UNIQUE (owner_id),
    CONSTRAINT ck_preference_currency CHECK (currency IN ('BRL', 'EUR')),
    CONSTRAINT ck_preference_theme CHECK (theme IN ('LIGHT', 'DARK', 'SYSTEM'))
);
