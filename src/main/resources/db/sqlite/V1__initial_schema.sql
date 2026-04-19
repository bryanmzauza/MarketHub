-- ============================================
-- MarketHub — V1: Schema Inicial (SQLite)
-- ============================================

-- Tabela de controle de versão do schema
CREATE TABLE IF NOT EXISTS schema_version (
    version     INTEGER NOT NULL,
    applied_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
);

-- Itens cadastrados no mercado do servidor
CREATE TABLE IF NOT EXISTS server_items (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    material        TEXT     NOT NULL,
    nbt_hash        TEXT     NULL,
    display_name    TEXT     NULL,
    category        TEXT     NULL,
    price_type      TEXT     NOT NULL,
    base_price      REAL     NOT NULL,
    current_price   REAL     NOT NULL,
    virtual_stock   INTEGER  NULL,
    target_stock    INTEGER  NULL,
    elasticity      REAL     NULL,
    price_min       REAL     NULL,
    price_max       REAL     NULL,
    enabled         INTEGER  DEFAULT 1,
    UNIQUE (material, nbt_hash)
);

-- 