package dev.brmz.markethub.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê e expõe os valores do config.yml de forma tipada.
 */
public class PluginConfig {

    // Database
    private String dbType;
    private String dbHost;
    private int dbPort;
    private String dbDatabase;
    private String dbUser;
    private String dbPassword;
    private int dbPoolSize;
    private Map<String, String> dbProperties;

    // Economy
    private String currencySymbol;
    private double serverBuyTax;
    private double serverSellTax;

    // Dynamic Pricing
    private int pricingUpdateIntervalSeconds;
    private double decayFactor;

    // Notifications
    private boolean notifyOnSale;
    private boolean notifyActionbar;
    private boolean notifyChat;

    // UI
    private int itemsPerPage;
    private int historyPoints;

    // Bedrock
    private boolean useGeyserForms;

    // NPC
    private boolean npcEnabled;
    private String npcName;
    private String npcSkin;

    public void load(FileConfiguration config) {
        // Database
        dbType = config.getString("database.type", "sqlite").toLowerCase();
        dbHost = config.getString("database.host", "localhost");
        dbPort = config.getInt("database.port", 3306);
        dbDatabase = config.getString("database.database", "markethub");
        dbUser = config.getString("database.user", "markethub_user");
        dbPassword = config.getString("database.password", "changeme");
        dbPoolSize = config.getInt("database.pool-size", 10);

        dbProperties = new HashMap<>();
        ConfigurationSection propsSection = config.getConfigurationSection("database.properties");
        if (propsSection != null) {
            for (String key : propsSection.getKeys(false)) {
                dbProperties.put(key, propsSection.getString(key));
            }
        }

        // Economy
        currencySymbol = config.getString("economy.currency-symbol", "$");
        serverBuyTax = config.getDouble("economy.server-buy-tax", 0.0);
        serverSellTax = config.getDouble("economy.server-sell-tax", 0.10);

        // Dynamic Pricing
        pricingUpdateIntervalSeconds = config.getInt("dynamic-pricing.update-interval-seconds", 60);
        decayFactor = config.getDouble("dynamic-pricing.decay-factor", 0.995);

        // Notifications
        notifyOnSale = config.getBoolean("notifications.on-sale", true);
        notifyActionbar = config.getBoolean("notifications.actionbar", true);
        notifyChat = config.getBoolean("notifications.chat", true);

        // UI
        itemsPerPage = config.getInt("ui.items-per-page", 45);
        historyPoints = config.getInt("ui.history-points", 30);

        // Bedrock
        useGeyserForms = config.getBoolean("bedrock.use-geyser-forms", true);

        // NPC
        npcEnabled = config.getBoolean("npc.enabled", true);
        npcName = config.getString("npc.name", "§6Mercador");
        npcSkin = config.getString("npc.skin", "default_trader");
    }

    /**
     * Monta o JDBC URL para MySQL.
     */
    public String getJdbcUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbDatabase
                + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    }

    public boolean isSQLite() {
        return "sqlite".equals(dbType);
    }

    // --- Getters ---

    public String getDbType() { return dbType; }
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbDatabase() { return dbDatabase; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public int getDbPoolSize() { return dbPoolSize; }
    public Map<String, String> getDbProperties() { return dbProperties; }

    public String getCurrencySymbol() { return currencySymbol; }
    public double getServerBuyTax() { return serverBuyTax; }
    public double getServerSellTax() { return serverSellTax; }

    public int getPricingUpdateIntervalSeconds() { return pricingUpdateIntervalSeconds; }
    public double getDecayFactor() { return decayFactor; }

    public boolean isNotifyOnSale() { return notifyOnSale; }
    public boolean isNotifyActionbar() { return notifyActionbar; }
    public boolean isNotifyChat() { return notifyChat; }

    public int getItemsPerPage() { return itemsPerPage; }
    public int getHistoryPoints() { return historyPoints; }

    public boolean isUseGeyserForms() { return useGeyserForms; }

    public boolean isNpcEnabled() { return npcEnabled; }
    public String getNpcName() { return npcName; }
    public String getNpcSkin() { return npcSkin; }

    /**
     * Carrega itens da loja a partir do arquivo shop.yml.
     */
    public static List<ServerItemConfig> loadShopItems(File shopFile) {
        List<ServerItemConfig> items = new ArrayList<>();
        if (!shopFile.exists()) return items;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(shopFile);
        ConfigurationSection itemsSection = yaml.getConfigurationSection("items");
        if (itemsSection == null) return items;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection sec = itemsSection.getConfigurationSection(key);
            if (sec == null) continue;
            ServerItemConfig item = new ServerItemConfig();
            item.material = sec.getString("material", key.toUpperCase());
            item.displayName = sec.getString("display-name", item.material);
            item.category = sec.getString("category", "geral");
            item.priceType = sec.getString("price-type", "DYNAMIC");
            item.basePrice = sec.getDouble("base-price", 10.0);
            item.virtualStock = sec.getLong("virtual-stock", 5000);
            item.targetStock = sec.getLong("target-stock", 5000);
            item.elasticity = sec.getDouble("elasticity", 0.3);
            item.priceMin = sec.getDouble("price-min", 1.0);
            item.priceMax = sec.getDouble("price-max", 100.0);
            items.add(item);
        }
        return items;
    }

    // --- Server Item Config Model ---

    public static class ServerItemConfig {
        public String material;
        public String displayName;
        public String category;
        public String priceType;
        public double basePrice;
        public long virtualStock;
        public long targetStock;
        public double elasticity;
        public double priceMin;
        public double priceMax;
    }
}
