-- ============================================
-- MarketHub — V1: Schema Inicial
-- ============================================

-- Tabela de controle de versão do schema
CREATE TABLE IF NOT EXISTS schema_version (
    version     INT NOT NULL,
    applied_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Itens cadastrados no mercado do servidor
CREATE TABLE IF NOT EXISTS server_items (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    material        VARCHAR(64)     NOT NULL,
    nbt_hash        VARCHAR(128)    NULL,
    display_name    VARCHAR(128)    NULL,
    category        VARCHAR(32)     NULL,
    price_type      VARCHAR(16)     NOT NULL COMMENT 'FIXED | DYNAMIC',
    base_price      DECIMAL(18,2)   NOT NULL,
    current_price   DECIMAL(18,2)   NOT NULL,
    virtual_stock   BIGINT          NULL,
    target_stock    BIGINT          NULL,
    elasticity      DECIMAL(4,2)    NULL,
    price_min       DECIMAL(18,2)   NULL,
    price_max       DECIMAL(18,2)   NULL,
    enabled         BOOLEAN         DEFAULT TRUE,
    UNIQUE KEY uk_material_nbt (material, nbt_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Marca versão 1 como aplicada
INSERT INTO schema_version (version) VALUES (1);
