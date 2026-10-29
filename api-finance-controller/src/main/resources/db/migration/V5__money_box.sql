-- Cofrinhos (reservas) por utilizador e moeda. Saldo e progresso são derivados na leitura; nada disso é guardado.
CREATE TABLE money_box (
    id          uuid           NOT NULL,
    owner_id    uuid           NOT NULL,
    name        varchar(100)   NOT NULL,
    description varchar(255),
    icon        varchar(40)    NOT NULL,
    color       varchar(7)     NOT NULL,
    currency    varchar(3)     NOT NULL,
    -- ausência de meta = NULL; meta 0 nunca é guardada
    goal        numeric(12, 2),
    created_at  timestamptz    NOT NULL,
    CONSTRAINT pk_money_box PRIMARY KEY (id),
    CONSTRAINT fk_money_box_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT ck_money_box_currency CHECK (currency IN ('BRL', 'EUR')),
    CONSTRAINT ck_money_box_goal CHECK (goal IS NULL OR (goal > 0 AND goal <= 9999999999.99))
);

-- listagem por moeda na ordem de criação
CREATE INDEX ix_money_box_list ON money_box (owner_id, currency, created_at, id);

-- Depósitos e saques; a moeda é a do cofrinho. Excluir o cofrinho remove as movimentações (RN-BOX-06).
CREATE TABLE money_box_entry (
    id           uuid           NOT NULL,
    owner_id     uuid           NOT NULL,
    money_box_id uuid           NOT NULL,
    kind         varchar(10)    NOT NULL,
    amount       numeric(12, 2) NOT NULL,
    entry_date   date           NOT NULL,
    note         varchar(255),
    created_at   timestamptz    NOT NULL,
    CONSTRAINT pk_money_box_entry PRIMARY KEY (id),
    CONSTRAINT fk_money_box_entry_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_money_box_entry_box FOREIGN KEY (money_box_id) REFERENCES money_box (id) ON DELETE CASCADE,
    CONSTRAINT ck_money_box_entry_kind CHECK (kind IN ('DEPOSIT', 'WITHDRAWAL')),
    CONSTRAINT ck_money_box_entry_amount CHECK (amount > 0 AND amount <= 9999999999.99),
    CONSTRAINT ck_money_box_entry_date CHECK (entry_date BETWEEN DATE '2000-01-01' AND DATE '2100-12-31')
);

-- histórico de um cofrinho (listagem e conferência do saldo) e cascata da exclusão
CREATE INDEX ix_money_box_entry_box ON money_box_entry (money_box_id, entry_date DESC, created_at DESC, id DESC);

-- Meta geral de reservas por moeda; ausência de linha = sem meta.
CREATE TABLE reserve_goal (
    id          uuid           NOT NULL,
    owner_id    uuid           NOT NULL,
    currency    varchar(3)     NOT NULL,
    goal_amount numeric(12, 2) NOT NULL,
    CONSTRAINT pk_reserve_goal PRIMARY KEY (id),
    CONSTRAINT fk_reserve_goal_owner FOREIGN KEY (owner_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT uk_reserve_goal_owner_currency UNIQUE (owner_id, currency),
    CONSTRAINT ck_reserve_goal_currency CHECK (currency IN ('BRL', 'EUR')),
    CONSTRAINT ck_reserve_goal_amount CHECK (goal_amount > 0 AND goal_amount <= 9999999999.99)
);
