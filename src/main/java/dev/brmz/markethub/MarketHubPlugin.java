package dev.brmz.markethub;

import dev.brmz.markethub.commands.AdminCommand;
import dev.brmz.markethub.commands.MarketHubCommand;
import dev.brmz.markethub.config.PluginConfig;
import dev.brmz.markethub.config.PluginConfig.ServerItemConfig;
import dev.brmz.markethub.database.DatabaseManager;
import dev.brmz.markethub.database.dao.ServerItemDAO;
import dev.brmz.markethub.economy.VaultHook;
import dev.brmz.markethub.listeners.NPCClickListener;
import dev.brmz.markethub.market.DynamicPricingEngine;
import dev.brmz.markethub.market.ServerMarketService;
import dev.brmz.markethub.npc.TraderNPCTrait;
import dev.brmz.markethub.notifications.NotificationService;
import dev.brmz.markethub.ui.MarketUI;
import dev.brmz.markethub.ui.bedrock.ServerShopForm;
import dev.brmz.markethub.ui.java.ServerShopGUI;
import dev.brmz.markethub.util.BedrockUtil;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class MarketHubPlugin extends JavaPlugin {

    private static MarketHubPlugin instance;

    private final PluginConfig pluginConfig = new PluginConfig();
    private DatabaseManager databaseManager;
    private VaultHook vaultHook;

    // Mercado do Servidor
    private ServerItemDAO serverItemDAO;
    private DynamicPricingEngine dynamicPricingEngine;
    private ServerMarketService serverMarketService;
    private ServerShopGUI serverShopGUI;
    private ServerShopForm serverShopForm;

    // UI + Comandos
    private MarketUI marketUI;
    private NotificationService notificationService;

    @Override
    public void onEnable() {
        instance = this;
        long start = System.currentTimeMillis();

        // 1. Configuração
        saveDefaultConfig();
        saveResource("shop.yml", false);
        pluginConfig.load(getConfig());
        getLogger().info("Configuração carregada.");

        // 2. Economia (Vault)
        vaultHook = new VaultHook(this);
        if (!vaultHook.setup()) {
            getLogger().severe("Falha ao conectar com Vault! Desabilitando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 3. Banco de dados (SQLite padrão / MySQL opcional)
        databaseManager = new DatabaseManager(getLogger(), pluginConfig, getClassLoader(), getDataFolder());
        if (!databaseManager.init()) {
            getLogger().severe("Falha ao inicializar banco de dados! Desabilitando plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // 4. Detecção Bedrock (Floodgate)
        BedrockUtil.init(getLogger());

        // 5. Mercado do Servidor
        serverItemDAO = new ServerItemDAO(databaseManager);
        loadServerItemsFromConfig();
        dynamicPricingEngine = new DynamicPricingEngine(this, serverItemDAO, pluginConfig);
        serverMarketService = new ServerMarketService(getLogger(), serverItemDAO, vaultHook,
                dynamicPricingEngine, pluginConfig);
        serverShopGUI = new ServerShopGUI(getLogger(), serverItemDAO, serverMarketService, vaultHook,
                p -> { if (marketUI != null) marketUI.openMainMenu(p); });
        try {
            serverShopForm = new ServerShopForm(getLogger(), serverItemDAO, serverMarketService, vaultHook);
        } catch (NoClassDefFoundError ignored) {
            // Geyser/Floodgate não presente — forms Bedrock desabilitados
        }

        // Iniciar task de preço dinâmico
        dynamicPricingEngine.startDecayTask(pluginConfig.getPricingUpdateIntervalSeconds());
        getLogger().info("Mercado do servidor inicializado.");

        // 6. NPC, UI, Comandos
        marketUI = new MarketUI(this);
        notificationService = new NotificationService();

        // Registrar comandos
        AdminCommand adminCmd = new AdminCommand(this, serverItemDAO);
        MarketHubCommand mainCmd = new MarketHubCommand(this, marketUI, adminCmd);
        PluginCommand mhCommand = getCommand("mh");
        if (mhCommand != null) {
            mhCommand.setExecutor(mainCmd);
            mhCommand.setTabCompleter(mainCmd);
        }

        // Registrar NPC trait (Citizens)
        try {
            CitizensAPI.getTraitFactory().registerTrait(
                    net.citizensnpcs.api.trait.TraitInfo.create(TraderNPCTrait.class)
                            .withName("markethub_trader"));
            getServer().getPluginManager().registerEvents(new NPCClickListener(marketUI), this);
            getLogger().info("Citizens NPC trait registrado.");
        } catch (NoClassDefFoundError e) {
            getLogger().info("Citizens não encontrado — NPC desabilitado.");
        }

        long elapsed = System.currentTimeMillis() - start;
        getLogger().info("MarketHub v" + getDescription().getVersion() + " habilitado em " + elapsed + "ms!");
    }

    /**
     * Carrega itens do shop.yml para o banco de dados.
     * Itens já existentes (por material) não são sobrescritos.
     */
    private void loadServerItemsFromConfig() {
        List<ServerItemConfig> items = PluginConfig.loadShopItems(new java.io.File(getDataFolder(), "shop.yml"));
        if (items.isEmpty()) return;

        int inserted = 0;
        for (ServerItemConfig cfg : items) {
            try {
                if (serverItemDAO.existsByMaterial(cfg.material)) continue;

                ServerItemDAO.ServerItem item = new ServerItemDAO.ServerItem();
                item.material = cfg.material;
                item.displayName = cfg.displayName;
                item.category = cfg.category;
                item.priceType = cfg.priceType;
                item.basePrice = cfg.basePrice;
                item.currentPrice = cfg.basePrice;
                item.virtualStock = cfg.virtualStock;
                item.targetStock = cfg.targetStock;
                item.elasticity = cfg.elasticity;
                item.priceMin = cfg.priceMin;
                item.priceMax = cfg.priceMax;
                item.enabled = true;

                serverItemDAO.insert(item);
                inserted++;
            } catch (Exception e) {
                getLogger().warning("[ServerItems] Erro ao inserir item " + cfg.material + ": " + e.getMessage());
            }
        }

        if (inserted > 0) {
            getLogger().info("[ServerItems] " + inserted + " itens carregados do shop.yml.");
        }
    }

    @Override
    public void onDisable() {
        if (dynamicPricingEngine != null) {
            dynamicPricingEngine.stopDecayTask();
        }

        if (databaseManager != null) {
            databaseManager.shutdown();
        }

        getLogger().info("MarketHub desabilitado.");
        instance = null;
    }

    public static MarketHubPlugin getInstance() {
        return instance;
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public ServerItemDAO getServerItemDAO() {
        return serverItemDAO;
    }

    public DynamicPricingEngine getDynamicPricingEngine() {
        return dynamicPricingEngine;
    }

    public ServerMarketService getServerMarketService() {
        return serverMarketService;
    }

    public ServerShopGUI getServerShopGUI() {
        return serverShopGUI;
    }

    public ServerShopForm getServerShopForm() {
        return serverShopForm;
    }

    public MarketUI getMarketUI() {
        return marketUI;
    }

    public NotificationService getNotificationService() {
        return notificationService;
    }
}
