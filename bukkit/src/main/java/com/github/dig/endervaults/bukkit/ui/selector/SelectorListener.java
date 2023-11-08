package com.github.dig.endervaults.bukkit.ui.selector;

import com.github.dig.endervaults.api.VaultPluginProvider;
import com.github.dig.endervaults.api.permission.UserPermission;
import com.github.dig.endervaults.api.vault.Vault;
import com.github.dig.endervaults.api.vault.VaultRegistry;
import com.github.dig.endervaults.api.vault.metadata.VaultDefaultMetadata;
import com.github.dig.endervaults.bukkit.ui.icon.SelectIconInventory;
import com.github.dig.endervaults.bukkit.vault.BukkitVault;
import com.github.dig.endervaults.bukkit.vault.BukkitVaultFactory;
import com.saicone.rtag.RtagItem;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

public class SelectorListener implements Listener {

    private final VaultRegistry registry = VaultPluginProvider.getPlugin().getRegistry();
    private final UserPermission permission = VaultPluginProvider.getPlugin().getPermission();
    private final FileConfiguration configuration = (FileConfiguration) VaultPluginProvider.getPlugin().getConfigFile().getConfiguration();

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getClickedInventory();
        ItemStack item = event.getCurrentItem();
        ClickType type = event.getClick();

        if (inventory != null && item != null && item.getType() != Material.AIR) {
            RtagItem tag = RtagItem.of(item);
            if (tag.hasTag(SelectorConstants.NBT_VAULT_ITEM)) {
                event.setCancelled(true);
                if (tag.hasTag(SelectorConstants.NBT_VAULT_ID) && tag.hasTag(SelectorConstants.NBT_VAULT_OWNER_UUID)) {
                    UUID vaultID = tag.getOptional(SelectorConstants.NBT_VAULT_ID).asUuid();
                    UUID vaultOwnerUUID = tag.getOptional(SelectorConstants.NBT_VAULT_OWNER_UUID).asUuid();

                    registry.get(vaultOwnerUUID, vaultID).ifPresent(vault -> {
                        BukkitVault bukkitVault = (BukkitVault) vault;
                        if (type == ClickType.LEFT) {
                            if (permission.canUseVault(player, (int) bukkitVault.getMetadata().get(VaultDefaultMetadata.ORDER.getKey()))) {
                                bukkitVault.launchFor(player);
                            }
                        } else if (type == ClickType.RIGHT && configuration.getBoolean("selector.select-icon.enabled", true) && permission.canSelectIcon(player)) {
                            new SelectIconInventory(vault).launchFor(player);
                        }
                    });
                } else if (tag.hasTag(SelectorConstants.NBT_VAULT_ORDER) && tag.hasTag(SelectorConstants.NBT_VAULT_OWNER_UUID)) {
                    int orderValue = tag.get(SelectorConstants.NBT_VAULT_ORDER);
                    UUID vaultOwnerUUID = tag.getOptional(SelectorConstants.NBT_VAULT_OWNER_UUID).asUuid();

                    if (!permission.canUseVault(player, orderValue) && !permission.isVaultAdmin(player)) {
                        return;
                    }

                    Optional<Vault> vaultOptional = registry
                            .getByMetadata(vaultOwnerUUID, VaultDefaultMetadata.ORDER.getKey(), orderValue);

                    BukkitVault vault;
                    if (vaultOptional.isPresent()) {
                        vault = (BukkitVault) vaultOptional.get();
                    } else {
                        vault = (BukkitVault) BukkitVaultFactory.create(vaultOwnerUUID, new HashMap<String, Object>(){{
                            put(VaultDefaultMetadata.ORDER.getKey(), orderValue);
                        }});
                        registry.register(vaultOwnerUUID, vault);
                    }

                    vault.launchFor(player);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(InventoryMoveItemEvent event) {
        ItemStack item = event.getItem();
        if (item != null && item.getType() != Material.AIR) {
            if (RtagItem.of(item).hasTag(SelectorConstants.NBT_VAULT_ITEM)) {
                event.setCancelled(true);
            }
        }
    }
}
