-- V1: modelo inicial do Estoque Fácil
-- Datas em TIMESTAMPTZ (UTC); quantidades em NUMERIC (nunca ponto flutuante).

CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(120) NOT NULL,
    email         VARCHAR(160) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('MANAGER', 'OPERATOR', 'VIEWER')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_users_email ON users (lower(email));

CREATE TABLE category (
    id     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name   VARCHAR(80) NOT NULL,
    active BOOLEAN     NOT NULL DEFAULT TRUE
);
CREATE UNIQUE INDEX ux_category_name ON category (lower(trim(name)));

CREATE TABLE supplier (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(120) NOT NULL,
    contact_name VARCHAR(120),
    email        VARCHAR(160),
    phone        VARCHAR(30),
    notes        TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE product (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    sku         VARCHAR(60)  NOT NULL,
    name        VARCHAR(160) NOT NULL,
    description TEXT,
    category_id UUID REFERENCES category (id),
    unit        VARCHAR(5)   NOT NULL CHECK (unit IN ('UN', 'KG', 'L')),
    min_stock   NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (min_stock >= 0),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- SKU único ignorando espaços extras e maiúsculas/minúsculas
CREATE UNIQUE INDEX ux_product_sku ON product (upper(trim(sku)));
CREATE INDEX ix_product_name ON product (lower(name));
CREATE INDEX ix_product_category ON product (category_id);

-- Ledger imutável: fonte auditável de toda alteração de saldo
CREATE TABLE stock_movement (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id            UUID        NOT NULL REFERENCES product (id),
    type                  VARCHAR(20) NOT NULL
                          CHECK (type IN ('ENTRY', 'EXIT', 'ADJUSTMENT_IN', 'ADJUSTMENT_OUT')),
    status                VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED'
                          CHECK (status IN ('DRAFT', 'CONFIRMED')),
    quantity              NUMERIC(14, 3) NOT NULL CHECK (quantity > 0),
    reason                VARCHAR(255) NOT NULL,
    supplier_id           UUID REFERENCES supplier (id),
    reference_movement_id UUID REFERENCES stock_movement (id),
    actor_id              UUID        NOT NULL REFERENCES users (id),
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_movement_product_date ON stock_movement (product_id, created_at DESC);
CREATE INDEX ix_movement_actor ON stock_movement (actor_id);
CREATE INDEX ix_movement_type_date ON stock_movement (type, created_at DESC);

-- Movimentações confirmadas não podem ser alteradas nem removidas;
-- correções são feitas por movimentação compensatória.
CREATE FUNCTION forbid_confirmed_movement_change() RETURNS trigger AS $$
BEGIN
    IF OLD.status = 'CONFIRMED' THEN
        RAISE EXCEPTION 'Movimentação confirmada é imutável (id=%)', OLD.id;
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_movement_immutable
    BEFORE UPDATE OR DELETE ON stock_movement
    FOR EACH ROW EXECUTE FUNCTION forbid_confirmed_movement_change();

-- Projeção do saldo atual (uma linha por produto), com lock otimista via version
CREATE TABLE stock_balance (
    product_id UUID PRIMARY KEY REFERENCES product (id),
    quantity   NUMERIC(14, 3) NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    version    BIGINT         NOT NULL DEFAULT 0
);