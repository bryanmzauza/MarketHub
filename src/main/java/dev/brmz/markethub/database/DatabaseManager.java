package dev.brmz.markethub.database;

import dev.brmz.markethub.config.PluginConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gerencia o pool de conexões HikariCP e inicializa o banco de dados.
 * Suporta SQLite (padrão) e MySQL.
 */
public class DatabaseManager {

    private final Logger logger;
    private final PluginConfig config;
    private final ClassLoader classLoader;
    private final File dataFolder;
    private HikariDataSource dataSource;
    private boolean sqlite;

    public DatabaseManager(Logger logger, PluginConfig config, ClassLoader classLoader, File dataFolder) {
        this.logger = logger;
        this.config = config;
        this.classLoader = classLoader;
        this.dataFolder = dataFolder;
        this.sqlite = config.isSQLite();
    }

    /**
     * Inicializa o pool de conexões e executa as migrations.
     * @return true se a inicialização foi bem-sucedida
     */
    public boolean init() {
        try {
            setupPool();
            if (sqlite) {
                configureSQLite();
            }
            runMigrations();
            logger.info("[MarketHub] Banco de dados inicializado com sucesso (" + (sqlite ? "SQLite" : "MySQL") + ").");
            return true;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "[MarketHub] Falha ao inicializar banco de dados!", e);
            return false;
        }
    }

    private void setupPool() {
        HikariConfig hikari = new HikariConfig();
        hikari.setPoolName("MarketHub-Pool");
        hikari.setConnectionTimeout(5000);

        if (sqlite) {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "markethub.db");
            hikari.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikari.setMaximumPoolSize(1);
            hikari.setMinimumIdle(1);
            hikari.setIdleTimeout(0);
            hikari.setMaxLifetime(0);
        } else {
            hikari.setJdbcUrl(config.getJdbcUrl());
            hikari.setUsername(config.getDbUser());
            hikari.setPassword(config.getDbPassword());
            hikari.setMaximumPoolSize(config.getDbPoolSize());
            hikari.setIdleTimeout(300000);
            hikari.setMaxLifetime(600000);
            hikari.setMinimumIdle(2);

            Map<String, String> props = config.getDbProperties();
            if (props != null) {
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    hikari.addDataSourceProperty(entry.getKey(), entry.getValue());
                }
            }
        }

        dataSource = new HikariDataSource(hikari);
    }

    private void configureSQLite() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");
            stmt.execute("PRAGMA foreign_keys=ON");
        }
    }

    private void runMigrations() throws SQLException {
        MigrationRunner runner = new MigrationRunner(logger, classLoader, sqlite);
        try (Connection conn = dataSource.getConnection()) {
            runner.migrate(conn);
        }
    }

    public boolean isSQLite() {
        return sqlite;
    }

    /**
     * Obtém uma conexão do pool. Sempre feche após o uso (try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Pool de conexões não está disponível.");
        }
        return dataSource.getConnection();
    }

    /**
     * Fecha o pool de conexões. Chamar em onDisable().
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("[MarketHub] Pool de conexões encerrado.");
        }
    }
}
