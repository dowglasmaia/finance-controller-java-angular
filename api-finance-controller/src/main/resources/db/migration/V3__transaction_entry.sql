CREATE TABLE transaction_entry (
    id             uuid           NOT NULL,
    owner_id       uuid           NOT NULL,
    kind           varchar(10)    NOT NULL,
    description    varchar(120)   NOT NULL,
    amount         numeric(12, 2) NOT NULL,
    currency       varchar(3)     NOT NULL,
    entry_date     date           NOT NULL,
    category_id    uuid           NOT NULL,
    -- excluir a subcategoria deixa o lançamento sem subcategoria (P-11)
    subcategory_id uuid,
    notes          varchar(500),
    created_at     timestamptz    NOT NULL,
    updated_at     timestamptz    NOT NULL,
    CONSTRAINT pk_transaction_entry PRIMARY KEY (id),
    CONSTRAINT fk_transaction_entry_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    -- NO ACTION (verificado no fim do comando): excluir o utilizador remove lançamentos e categorias juntos
    CONSTRAINT fk_transaction_entry_category FOREIGN KEY (category_id) REFERENCES category (id),
    CONSTRAINT fk_transaction_entry_subcategory FOREIGN KEY (subcategory_id) REFERENCES subcategory (id) ON DELETE SET NULL,
    CONSTRAINT ck_transaction_entry_kind CHECK (kind IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_transaction_entry_amount CHECK (amount > 0 AND amount <= 9999999999.99),
    CONSTRAINT ck_transaction_entry_currency CHECK (currency IN ('BRL', 'EUR')),
    CONSTRAINT ck_transaction_entry_date CHECK (entry_date BETWEEN DATE '2000-01-01' AND DATE '2100-12-31')
);

-- listagem mensal por moeda na ordem RN-TXN-08 e somas do mês
CREATE INDEX ix_transaction_entry_list ON transaction_entry (owner_id, currency, entry_date DESC, created_at DESC, id DESC);
-- verificação "categoria em uso" e RESTRICT/NO ACTION da categoria
CREATE INDEX ix_transaction_entry_category ON transaction_entry (category_id);
-- ON DELETE SET NULL da subcategoria
CREATE INDEX ix_transaction_entry_subcategory ON transaction_entry (subcategory_id) WHERE subcategory_id IS NOT NULL;
