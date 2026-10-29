-- Meta recorrente (global) por utilizador e moeda; ausência de linha = sem meta global.
CREATE TABLE budget_setting (
    id            uuid           NOT NULL,
    owner_id      uuid           NOT NULL,
    currency      varchar(3)     NOT NULL,
    monthly_limit numeric(12, 2) NOT NULL,
    CONSTRAINT pk_budget_setting PRIMARY KEY (id),
    CONSTRAINT fk_budget_setting_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_budget_setting_owner_currency UNIQUE (owner_id, currency),
    CONSTRAINT ck_budget_setting_currency CHECK (currency IN ('BRL', 'EUR')),
    CONSTRAINT ck_budget_setting_limit CHECK (monthly_limit > 0 AND monthly_limit <= 9999999999.99)
);

-- Ajuste de um mês: um valor (> 0) ou "sem meta neste mês" (disabled); ausência de linha = usa a meta recorrente.
CREATE TABLE budget_month_override (
    id              uuid           NOT NULL,
    owner_id        uuid           NOT NULL,
    currency        varchar(3)     NOT NULL,
    reference_month varchar(7)     NOT NULL,
    limit_amount    numeric(12, 2),
    disabled        boolean        NOT NULL DEFAULT false,
    CONSTRAINT pk_budget_month_override PRIMARY KEY (id),
    CONSTRAINT fk_budget_month_override_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_budget_month_override UNIQUE (owner_id, currency, reference_month),
    CONSTRAINT ck_budget_month_override_currency CHECK (currency IN ('BRL', 'EUR')),
    CONSTRAINT ck_budget_month_override_month CHECK (reference_month ~ '^\d{4}-(0[1-9]|1[0-2])$'),
    CONSTRAINT ck_budget_month_override_value CHECK (
        (disabled AND limit_amount IS NULL)
        OR (NOT disabled AND limit_amount > 0 AND limit_amount <= 9999999999.99))
);

-- Limite recorrente por categoria; excluir a categoria remove o limite dela.
CREATE TABLE budget_category_limit (
    id           uuid           NOT NULL,
    owner_id     uuid           NOT NULL,
    currency     varchar(3)     NOT NULL,
    category_id  uuid           NOT NULL,
    limit_amount numeric(12, 2) NOT NULL,
    CONSTRAINT pk_budget_category_limit PRIMARY KEY (id),
    CONSTRAINT fk_budget_category_limit_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_budget_category_limit_category FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE CASCADE,
    CONSTRAINT uk_budget_category_limit UNIQUE (owner_id, currency, category_id),
    CONSTRAINT ck_budget_category_limit_currency CHECK (currency IN ('BRL', 'EUR')),
    CONSTRAINT ck_budget_category_limit_value CHECK (limit_amount > 0 AND limit_amount <= 9999999999.99)
);

-- ON DELETE CASCADE da categoria
CREATE INDEX ix_budget_category_limit_category ON budget_category_limit (category_id);
