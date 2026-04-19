package dev.brmz.markethub.util;

import org.bukkit.inventory.ItemStack;

/**
 * Utilitário para serializar/desserializar ItemStack para armazenamento em banco.
 * Usa a API nativa do Paper (serializeAsBytes/deserializeBytes).
 */
public class ItemSerializer {

    /**
     * Serializa um ItemStack para bytes (compatível com BLOB no MySQL).
     */
    public static byte[] serialize(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        return itemStack.serializeAsBytes();
    }

    /**
     * Desserializa bytes para um ItemStack.
     */
    public static ItemStack deserialize(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        return ItemStack.deserializeBytes(data);
    }
}
