package dev.brmz.markethub.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Executa migrations SQL sequenciais com base em uma tabela schema_version.
 * Arquivos de migração ficam em resources/db/ com formato V{n}__descricao.sql
 */
public class MigrationRunner {

    private static final int LATEST_VERSION = 1;

    private final Logger logger;
    private final ClassLoader classLoader;
    private final boolean sqlite;

    public MigrationRunner(Logger logger, ClassLoader classLoader, boolean sqlite) {
        this.logger = logger;
        this.classLoader = classLoader;
        this.sqlite = sqlite;
    }

    /**
     * Verifica a versão atual do schema e aplica migrations pendentes.
     */
    public void migrate(Connection connection) throws SQLException {
        int currentVersion = getCurrentVersion(connection);
        logger.info("[MarketHub] Schema atual: v" + currentVersion + " | Alvo: v" + LATEST_VERSION);

        for (int v = currentVersion + 1; v <= LATEST_VERSION; v++) {
            applyMigration(connection, v);
        }

        if (currentVersion >= LATEST_VERSION) {
            logger.info("[MarketHub] Banco de dados já está atualizado.");
        }
    }

    private int getCurrentVersion(Connection connection) throws SQLException {
        // Verifica se a tabela schema_version existe
        try (ResultSet rs = connection.getMetaData().getTables(null, null, "schema_version", null)) {
            if (!rs.next()) {
                return 0; // Tabela não existe → versão 0
            }
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT MAX(version) FROM schema_version")) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private void applyMigration(Connection connection, int version) throws SQLException {
        String fileName = findMigrationFile(version);
        if (fileName == null) {
            throw new SQLException("Arquivo de migração não encontrado para versão " + version);
        }

        logger.info("[MarketHub] Aplicando migração: " + fileName);

        String sql = readResource((sqlite ? "db/sqlite/" : "db/") + fileName);
        if (sql == null || sql.isBlank()) {
            throw new SQLException("Arquivo de migração vazio: " + fileName);
        }

        connection.setAutoCommit(false);
        try {
            // Divide por ';' e executa cada statement
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            connection.commit();
            logger.info("[MarketHub] Migração v" + version + " aplicada com sucesso.");
        } catch (SQLException e) {
            connection.rollback();
            throw new SQLException("Falha na migração v" + version + ": " + e.getMessage(), e);
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Procura o arquivo de migração V{version}__*.sql no classpath.
     */
    private String findMigrationFile(int version) {
        // Convenção: V1__initial_schema.sql, V2__add_something.sql, etc.
        String prefix = "V" + version + "__";
        // Tentamos padrões conhecidos. Para um sistema mais robusto,
        // listaríamos o diretório, mas em JARs isso é complexo.
        String[] knownMigrations = {
            "V1__initial_schema.sql"
        };

        for (String name : knownMigrations) {
            if (name.startsWith(prefix)) {
                return name;
            }
        }
        return null;
    }

    private String readResource(String path) {
        try (InputStream is = classLoader.getResourceAsStream(path)) {
            if (is == null) {
                logger.warning("[MarketHub] Recurso não encontrado: " + path);
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    // Ignora linhas de comentário puro
                    String trimmed = line.trim();
                    if (trimmed.startsWith("--")) {
                        continue;
                    }
                    sb.append(line).append("\n");
                }
                return sb.toString();
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "[MarketHub] Erro ao ler recurso: " + path, e);
            return null;
        }
    }
}
