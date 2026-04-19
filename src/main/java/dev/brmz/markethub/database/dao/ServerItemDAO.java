package dev.brmz.markethub.database.dao;

import dev.brmz.markethub.database.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ServerItemDAO {

    private final DatabaseManager db;

    public ServerItemDAO(DatabaseManager db) {
        this.db = db;
    }

    // --- Model interno ---

    public static class ServerItem {
        public int id;
        public String material;
        public String nbtHash;
        public String displayName;
        public String category;
        public String priceType; // FIXED | DYNAMIC
        public double basePrice;
        public double currentPrice;
        public long virtualStock;
        public long targetStock;
        public double elasticity;
        public double priceMin;
        public double priceMax;
        public boolean enabled;
    }

    // --- CRUD ---

    public int insert(ServerItem item) throws SQLException {
        String sql = """
            INSERT INTO server_items
                (material, nbt_hash, display_name, category, price_type,
                 base_price, current_price, virtual_stock, target_stock,
                 elasticity, price_min, price_max, enabled)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.material);
            ps.setString(2, item.nbtHash);
            ps.setString(3, item.displayName);
            ps.setString(4, item.category);
            ps.setString(5, item.priceType);
            ps.setDouble(6, item.basePrice);
            ps.setDouble(7, item.currentPrice);
            ps.setLong(8, item.virtualStock);
            ps.setLong(9, item.targetStock);
            ps.setDouble(10, item.elasticity);
            ps.setDouble(11, item.priceMin);
            ps.setDouble(12, item.priceMax);
            ps.setBoolean(13, item.enabled);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        }
    }

    public Optional<ServerItem> findById(int id) throws SQLException {
        String sql = "SELECT * FROM server_items WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<ServerItem> findByMaterial(String material) throws SQLException {
        String sql = "SELECT * FROM server_items WHERE material = ? AND enabled = TRUE LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, material);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public boolean existsByMaterial(String material) throws SQLException {
        String sql = "SELECT 1 FROM server_items WHERE material = ? LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, material);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<ServerItem> findByCategory(String category) throws SQLException {
        String sql = "SELECT * FROM server_items WHERE category = ? AND enabled = TRUE ORDER BY display_name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            return mapRows(ps);
        }
    }

    public List<ServerItem> listAllEnabled() throws SQLException {
        String sql = "SELECT * FROM server_items WHERE enabled = TRUE ORDER BY category, display_name";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return mapRows(ps);
        }
    }

    public List<String> listCategories() throws SQLException {
        String sql = "SELECT DISTINCT category FROM server_items WHERE enabled = TRUE AND category IS NOT NULL ORDER BY category";
        List<String> categories = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        }
        return categories;
    }

    public void updatePrice(int id, double newPrice) throws SQLException {
        String sql = "UPDATE server_items SET current_price = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateStock(int id, long newStock) throws SQLException {
        String sql = "UPDATE server_items SET virtual_stock = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newStock);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void updateStockAndPrice(int id, long newStock, double newPrice) throws SQLException {
        String sql = "UPDATE server_items SET virtual_stock = ?, current_price = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newStock);
            ps.setDouble(2, newPrice);
            ps.setInt(3, id);
            ps.executeUpdate();
        }
    }

    public void setEnabled(int id, boolean enabled) throws SQLException {
        String sql = "UPDATE server_items SET enabled = ? WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, enabled);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public List<ServerItem> findAllDynamic() throws SQLException {
        String sql = "SELECT * FROM server_items WHERE price_type = 'DYNAMIC' AND enabled = TRUE";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return mapRows(ps);
        }
    }

    // --- Mapping ---

    private List<ServerItem> mapRows(PreparedStatement ps) throws SQLException {
        List<ServerItem> list = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    private ServerItem mapRow(ResultSet rs) throws SQLException {
        ServerItem item = new ServerItem();
        item.id = rs.getInt("id");
        item.material = rs.getString("material");
        item.nbtHash = rs.getString("nbt_hash");
        item.displayName = rs.getString("display_name");
        item.category = rs.getString("category");
        item.priceType = rs.getString("price_type");
        item.basePrice = rs.getDouble("base_price");
        item.currentPrice = rs.getDouble("current_price");
        item.virtualStock = rs.getLong("virtual_stock");
        item.targetStock = rs.getLong("target_stock");
        item.elasticity = rs.getDouble("elasticity");
        item.priceMin = rs.getDouble("price_min");
        item.priceMax = rs.getDouble("price_max");
        item.enabled = rs.getBoolean("enabled");
        return item;
    }
}
