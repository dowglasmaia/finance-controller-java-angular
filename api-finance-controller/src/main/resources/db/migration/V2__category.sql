CREATE TABLE category (
    id         uuid         NOT NULL,
    owner_id   uuid         NOT NULL,
    name       varchar(60)  NOT NULL,
    -- unicidade ignorando caixa e espaços nas pontas
    name_key   varchar(60)  GENERATED ALWAYS AS (lower(btrim(name))) STORED,
    type       varchar(10)  NOT NULL,
    icon       varchar(40)  NOT NULL,
    color      varchar(7)   NOT NULL,
    created_at timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_category PRIMARY KEY (id),
    CONSTRAINT fk_category_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_category_owner_name UNIQUE (owner_id, name_key),
    CONSTRAINT ck_category_type CHECK (type IN ('INCOME', 'EXPENSE', 'BOTH')),
    CONSTRAINT ck_category_color CHECK (color ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE TABLE subcategory (
    id           uuid         NOT NULL,
    category_id  uuid         NOT NULL,
    name         varchar(60)  NOT NULL,
    name_key     varchar(60)  GENERATED ALWAYS AS (lower(btrim(name))) STORED,
    is_protected boolean      NOT NULL DEFAULT false,
    created_at   timestamptz  NOT NULL DEFAULT now(),
    CONSTRAINT pk_subcategory PRIMARY KEY (id),
    CONSTRAINT fk_subcategory_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE,
    CONSTRAINT uk_subcategory_category_name UNIQUE (category_id, name_key)
);

CREATE INDEX ix_category_owner_id ON category (owner_id);
CREATE INDEX ix_subcategory_category_id ON subcategory (category_id);
