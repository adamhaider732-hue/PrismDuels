package com.prismsmp.duels.kit;

import com.prismsmp.duels.PrismDuels;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KitManager {
    private final PrismDuels plugin;
    private final File kitFile;
    private final Map<GameMode, List<ItemStack>> kits = new EnumMap<>(GameMode.class);
    private final Map<GameMode, ItemStack[]> kitArmor = new EnumMap<>(GameMode.class);
    private final Map<GameMode, ItemStack> kitOffhand = new EnumMap<>(GameMode.class);

    public KitManager(PrismDuels plugin) {
        this.plugin = plugin;
        this.kitFile = new File(plugin.getDataFolder(), "kits.yml");
        loadKits();
    }

    public void loadKits() {
        kits.clear(); kitArmor.clear(); kitOffhand.clear();
        if (!kitFile.exists()) generateDefaults();
        FileConfiguration config = YamlConfiguration.loadConfiguration(kitFile);
        for (GameMode mode : GameMode.values()) {
            ConfigurationSection section = config.getConfigurationSection(mode.getConfigKey());
            if (section == null) { loadDefaultKit(mode); continue; }
            List<ItemStack> items = new ArrayList<>();
            ConfigurationSection invSection = section.getConfigurationSection("inventory");
            if (invSection != null) for (String key : invSection.getKeys(false)) { ItemStack item = invSection.getItemStack(key); if (item != null) items.add(item); }
            ItemStack[] armor = new ItemStack[4];
            ConfigurationSection armorSection = section.getConfigurationSection("armor");
            if (armorSection != null) { armor[3] = armorSection.getItemStack("helmet"); armor[2] = armorSection.getItemStack("chestplate"); armor[1] = armorSection.getItemStack("leggings"); armor[0] = armorSection.getItemStack("boots"); }
            ItemStack offhand = section.getItemStack("offhand");
            kits.put(mode, items); kitArmor.put(mode, armor); if (offhand != null) kitOffhand.put(mode, offhand);
        }
        plugin.getLogger().info("Loaded kits for " + kits.size() + " gamemode(s).");
    }

    public void applyKit(Player player, GameMode mode) {
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        List<ItemStack> items = kits.get(mode); ItemStack[] armor = kitArmor.get(mode); ItemStack offhand = kitOffhand.get(mode);
        if (items == null || armor == null) { loadDefaultKit(mode); items = kits.get(mode); armor = kitArmor.get(mode); offhand = kitOffhand.get(mode); }
        if (armor != null) player.getInventory().setArmorContents(armor);
        if (offhand != null) player.getInventory().setItemInOffHand(offhand);
        if (items != null) for (int i = 0; i < items.size() && i < 36; i++) player.getInventory().setItem(i, items.get(i).clone());
        player.setHealth(player.getMaxHealth()); player.setFoodLevel(20); player.setSaturation(20f);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
    }

    private void generateDefaults() { for (GameMode mode : GameMode.values()) loadDefaultKit(mode); saveKits(); }

    public void saveKits() {
        FileConfiguration config = new YamlConfiguration();
        for (GameMode mode : GameMode.values()) {
            List<ItemStack> items = kits.get(mode); ItemStack[] armor = kitArmor.get(mode); ItemStack offhand = kitOffhand.get(mode);
            if (items == null) continue; String base = mode.getConfigKey();
            for (int i = 0; i < items.size(); i++) config.set(base + ".inventory.slot" + i, items.get(i));
            if (armor != null) { config.set(base + ".armor.helmet", armor[3]); config.set(base + ".armor.chestplate", armor[2]); config.set(base + ".armor.leggings", armor[1]); config.set(base + ".armor.boots", armor[0]); }
            if (offhand != null) config.set(base + ".offhand", offhand);
        }
        try { config.save(kitFile); } catch (IOException e) { plugin.getLogger().severe("Failed to save kits: " + e.getMessage()); }
    }

    @SuppressWarnings("deprecation")
    private void loadDefaultKit(GameMode mode) {
        List<ItemStack> items = new ArrayList<>();
        ItemStack[] armor = new ItemStack[4];
        ItemStack offhand = null;

        switch (mode) {

            // ============ 1. AXE ============
            case AXE -> {
                armor[3] = new ItemStack(Material.DIAMOND_HELMET);
                armor[2] = new ItemStack(Material.DIAMOND_CHESTPLATE);
                armor[1] = new ItemStack(Material.DIAMOND_LEGGINGS);
                armor[0] = new ItemStack(Material.DIAMOND_BOOTS);
                items.add(new ItemStack(Material.DIAMOND_AXE));
                items.add(new ItemStack(Material.DIAMOND_SWORD));
                items.add(new ItemStack(Material.CROSSBOW));
                items.add(new ItemStack(Material.BOW));
                items.add(new ItemStack(Material.ARROW, 6));
                offhand = new ItemStack(Material.SHIELD);
            }

            // ============ 2. DIASMP ============
            case DIASMP -> {
                armor[3] = enchant(Material.DIAMOND_HELMET, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[2] = enchant(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                ItemStack legs = enchant(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                legs.addUnsafeEnchantment(Enchantment.SWIFT_SNEAK, 3);
                armor[1] = legs;
                ItemStack boots = enchant(Material.DIAMOND_BOOTS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);
                boots.addUnsafeEnchantment(Enchantment.DEPTH_STRIDER, 3);
                armor[0] = boots;
                items.add(enchant(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2, Enchantment.UNBREAKING, 3));
                items.add(enchant(Material.DIAMOND_AXE, Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3));
                ItemStack shield = new ItemStack(Material.SHIELD);
                shield.addUnsafeEnchantment(Enchantment.UNBREAKING, 3); shield.addUnsafeEnchantment(Enchantment.MENDING, 1);
                items.add(shield);
                items.add(new ItemStack(Material.GOLDEN_APPLE, 64));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 64));
                ItemStack pick = enchant(Material.NETHERITE_PICKAXE, Enchantment.SILK_TOUCH, 1, Enchantment.MENDING, 1, Enchantment.EFFICIENCY, 5);
                pick.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                items.add(pick);
                items.add(new ItemStack(Material.OAK_LOG, 64));
                items.add(new ItemStack(Material.COBWEB, 64));
                items.add(new ItemStack(Material.WATER_BUCKET));
                // Inv row 2
                items.add(new ItemStack(Material.WATER_BUCKET));
                items.add(new ItemStack(Material.WATER_BUCKET));
                items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.CHORUS_FRUIT, 64));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
                // Pots
                for (int i = 0; i < 16; i++) items.add(splashPot(PotionEffectType.STRENGTH, 1800, 1)); // Strength II
                for (int i = 0; i < 3; i++) items.add(splashPot(PotionEffectType.SPEED, 9600, 0)); // Speed I 8min
                for (int i = 0; i < 3; i++) items.add(splashPot(PotionEffectType.FIRE_RESISTANCE, 9600, 0)); // Fire Res 8min
                offhand = new ItemStack(Material.TOTEM_OF_UNDYING);
            }

            // ============ 3. SMP ============
            case SMP -> {
                armor[3] = enchant(Material.NETHERITE_HELMET, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[2] = enchant(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                ItemStack legs = enchant(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                legs.addUnsafeEnchantment(Enchantment.SWIFT_SNEAK, 3);
                armor[1] = legs;
                ItemStack boots = enchant(Material.NETHERITE_BOOTS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);
                armor[0] = boots;
                ItemStack sword1 = enchant(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2);
                sword1.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
                items.add(sword1);
                ItemStack sword2 = enchant(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5, Enchantment.FIRE_ASPECT, 2);
                sword2.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
                sword2.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
                items.add(sword2);
                items.add(enchant(Material.NETHERITE_AXE, Enchantment.SHARPNESS, 5));
                items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 64));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                ItemStack shield = new ItemStack(Material.SHIELD);
                shield.addUnsafeEnchantment(Enchantment.UNBREAKING, 3); shield.addUnsafeEnchantment(Enchantment.MENDING, 1);
                items.add(shield);
                // Pots
                for (int i = 0; i < 12; i++) items.add(splashPot(PotionEffectType.STRENGTH, 1800, 1));
                for (int i = 0; i < 12; i++) items.add(splashPot(PotionEffectType.SPEED, 1800, 1));
                for (int i = 0; i < 3; i++) items.add(splashPot(PotionEffectType.FIRE_RESISTANCE, 9600, 0));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
                offhand = new ItemStack(Material.TOTEM_OF_UNDYING);
            }

            // ============ 4. SWORD ============
            case SWORD -> {
                armor[3] = enchant(Material.DIAMOND_HELMET, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                armor[2] = enchant(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                armor[1] = enchant(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                armor[0] = enchant(Material.DIAMOND_BOOTS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                items.add(enchant(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 5));
                items.add(new ItemStack(Material.COOKED_BEEF, 5));
            }

            // ============ 5. DIAPOT ============
            case DIAPOT -> {
                armor[3] = enchant(Material.DIAMOND_HELMET, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                armor[2] = enchant(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                armor[1] = enchant(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                armor[0] = enchant(Material.DIAMOND_BOOTS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3);
                items.add(enchant(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 5));
                items.add(new ItemStack(Material.COOKED_BEEF, 5));
                for (int i = 0; i < 7; i++) items.add(splashPot(PotionEffectType.INSTANT_HEALTH, 1, 1));
                for (int i = 0; i < 19; i++) items.add(splashPot(PotionEffectType.INSTANT_HEALTH, 1, 1));
                for (int i = 0; i < 3; i++) items.add(splashPot(PotionEffectType.STRENGTH, 1800, 1));
                for (int i = 0; i < 3; i++) items.add(splashPot(PotionEffectType.SPEED, 1800, 1));
                for (int i = 0; i < 3; i++) items.add(splashPot(PotionEffectType.REGENERATION, 1800, 0));
            }

            // ============ 6. MACE ============
            case MACE -> {
                armor[3] = enchant(Material.NETHERITE_HELMET, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[2] = enchant(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[1] = enchant(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                ItemStack boots = enchant(Material.NETHERITE_BOOTS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);
                armor[0] = boots;
                items.add(enchant(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3));
                items.add(enchant(Material.NETHERITE_AXE, Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3));
                items.add(new ItemStack(Material.ELYTRA));
                ItemStack mace1 = enchant(Material.MACE, Enchantment.DENSITY, 5, Enchantment.UNBREAKING, 3);
                mace1.addUnsafeEnchantment(Enchantment.WIND_BURST, 1);
                items.add(mace1);
                items.add(enchant(Material.MACE, Enchantment.BREACH, 4, Enchantment.UNBREAKING, 3));
                ItemStack shield = new ItemStack(Material.SHIELD);
                shield.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                items.add(shield);
                items.add(new ItemStack(Material.GOLDEN_APPLE, 64));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                // Inv row 2
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.WIND_CHARGE, 64));
                items.add(new ItemStack(Material.WIND_CHARGE, 64));
                for (int i = 0; i < 10; i++) items.add(splashPot(PotionEffectType.STRENGTH, 1800, 1));
                for (int i = 0; i < 10; i++) items.add(splashPot(PotionEffectType.SPEED, 1800, 1));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
                // Shulker with 13 speed + 14 strength
                items.add(makePotShulker(13, 14));
                offhand = new ItemStack(Material.TOTEM_OF_UNDYING);
            }

            // ============ 7. SPEARMACE ============
            case SPEARMACE -> {
                armor[3] = enchant(Material.NETHERITE_HELMET, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[2] = enchant(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[1] = enchant(Material.NETHERITE_LEGGINGS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                ItemStack boots = enchant(Material.NETHERITE_BOOTS, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);
                armor[0] = boots;
                items.add(enchant(Material.NETHERITE_SWORD, Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3));
                items.add(enchant(Material.NETHERITE_AXE, Enchantment.SHARPNESS, 5, Enchantment.UNBREAKING, 3));
                ItemStack mace1 = enchant(Material.MACE, Enchantment.DENSITY, 5, Enchantment.UNBREAKING, 3);
                mace1.addUnsafeEnchantment(Enchantment.WIND_BURST, 1);
                items.add(mace1);
                // Netherite Spear with fallback
                ItemStack spear;
                try {
                    spear = new ItemStack(Material.valueOf("NETHERITE_SPEAR"));
                    spear.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
                    spear.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                    try {
                        Enchantment lunge = org.bukkit.Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft("lunge"));
                        if (lunge != null) spear.addUnsafeEnchantment(lunge, 3);
                    } catch (Exception ignored) {}
                } catch (Exception e) {
                    spear = new ItemStack(Material.TRIDENT);
                    spear.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
                    spear.addUnsafeEnchantment(Enchantment.LOYALTY, 3);
                    spear.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                }
                items.add(spear);
                ItemStack shield = new ItemStack(Material.SHIELD);
                shield.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
                items.add(shield);
                items.add(new ItemStack(Material.GOLDEN_APPLE, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                // Inv row 2
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.WIND_CHARGE, 64));
                items.add(new ItemStack(Material.WIND_CHARGE, 64));
                for (int i = 0; i < 10; i++) items.add(splashPot(PotionEffectType.STRENGTH, 1800, 1));
                for (int i = 0; i < 10; i++) items.add(splashPot(PotionEffectType.SPEED, 1800, 1));
                items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
                items.add(new ItemStack(Material.COOKED_BEEF, 64));
                items.add(makePotShulker(13, 14));
                offhand = new ItemStack(Material.TOTEM_OF_UNDYING);
            }

            // ============ 8. UHC ============
            case UHC -> {
                armor[3] = enchant(Material.DIAMOND_HELMET, Enchantment.PROTECTION, 3);
                armor[2] = enchant(Material.DIAMOND_CHESTPLATE, Enchantment.PROTECTION, 2);
                armor[1] = enchant(Material.DIAMOND_LEGGINGS, Enchantment.PROTECTION, 2);
                armor[0] = enchant(Material.DIAMOND_BOOTS, Enchantment.PROTECTION, 3);
                items.add(enchant(Material.DIAMOND_SWORD, Enchantment.SHARPNESS, 3));
                items.add(enchant(Material.DIAMOND_AXE, Enchantment.EFFICIENCY, 3));
                items.add(enchant(Material.BOW, Enchantment.POWER, 1));
                ItemStack xbow = new ItemStack(Material.CROSSBOW);
                xbow.addUnsafeEnchantment(Enchantment.PIERCING, 1);
                items.add(xbow);
                items.add(new ItemStack(Material.ARROW, 10));
                items.add(new ItemStack(Material.GOLDEN_APPLE, 14));
                items.add(enchant(Material.DIAMOND_PICKAXE, Enchantment.EFFICIENCY, 3));
                items.add(new ItemStack(Material.SHIELD));
                items.add(new ItemStack(Material.WATER_BUCKET));
                // Inv row 2
                items.add(new ItemStack(Material.WATER_BUCKET));
                items.add(new ItemStack(Material.WATER_BUCKET));
                items.add(new ItemStack(Material.WATER_BUCKET));
                items.add(new ItemStack(Material.LAVA_BUCKET));
                items.add(new ItemStack(Material.LAVA_BUCKET));
                items.add(new ItemStack(Material.COBWEB, 8));
                items.add(new ItemStack(Material.OAK_PLANKS, 64));
                items.add(new ItemStack(Material.OAK_PLANKS, 64));
            }

            // ============ 9. CRYSTAL ============
            case CRYSTAL -> {
                // 2 prot 4 (helm, chest) + 2 blast prot 4 (legs, boots)
                armor[3] = enchant(Material.NETHERITE_HELMET, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[2] = enchant(Material.NETHERITE_CHESTPLATE, Enchantment.PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                ItemStack legs = enchant(Material.NETHERITE_LEGGINGS, Enchantment.BLAST_PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                armor[1] = legs;
                ItemStack boots = enchant(Material.NETHERITE_BOOTS, Enchantment.BLAST_PROTECTION, 4, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1);
                boots.addUnsafeEnchantment(Enchantment.FEATHER_FALLING, 4);
                armor[0] = boots;
                // Hotbar
                items.add(new ItemStack(Material.RESPAWN_ANCHOR, 64));
                items.add(new ItemStack(Material.RESPAWN_ANCHOR, 64));
                items.add(new ItemStack(Material.GLOWSTONE, 64));
                items.add(new ItemStack(Material.GLOWSTONE, 64));
                items.add(new ItemStack(Material.END_CRYSTAL, 64));
                items.add(new ItemStack(Material.END_CRYSTAL, 64));
                items.add(new ItemStack(Material.OBSIDIAN, 64));
                items.add(new ItemStack(Material.OBSIDIAN, 64));
                // Inv
                items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
                items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
                items.add(new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                items.add(new ItemStack(Material.ENDER_PEARL, 64));
                // 14 totems
                for (int i = 0; i < 14; i++) items.add(new ItemStack(Material.TOTEM_OF_UNDYING));
                // Shulker full of totems (27)
                items.add(makeTotemShulker());
                offhand = new ItemStack(Material.TOTEM_OF_UNDYING);
            }
        }

        kits.put(mode, items); kitArmor.put(mode, armor); if (offhand != null) kitOffhand.put(mode, offhand);
    }

    // ============ HELPERS ============

    private ItemStack enchant(Material mat, Object... enchants) {
        ItemStack item = new ItemStack(mat);
        for (int i = 0; i < enchants.length - 1; i += 2) {
            item.addUnsafeEnchantment((Enchantment) enchants[i], (int) enchants[i + 1]);
        }
        return item;
    }

    private ItemStack splashPot(PotionEffectType effect, int duration, int amplifier) {
        ItemStack pot = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) pot.getItemMeta();
        meta.addCustomEffect(new PotionEffect(effect, duration, amplifier), true);
        pot.setItemMeta(meta);
        return pot;
    }

    private ItemStack makePotShulker(int speedCount, int strengthCount) {
        ItemStack shulker = new ItemStack(Material.PURPLE_SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        for (int i = 0; i < speedCount; i++) box.getInventory().addItem(splashPot(PotionEffectType.SPEED, 1800, 1));
        for (int i = 0; i < strengthCount; i++) box.getInventory().addItem(splashPot(PotionEffectType.STRENGTH, 1800, 1));
        meta.setBlockState(box);
        shulker.setItemMeta(meta);
        return shulker;
    }

    private ItemStack makeTotemShulker() {
        ItemStack shulker = new ItemStack(Material.PURPLE_SHULKER_BOX);
        BlockStateMeta meta = (BlockStateMeta) shulker.getItemMeta();
        ShulkerBox box = (ShulkerBox) meta.getBlockState();
        for (int i = 0; i < 27; i++) box.getInventory().addItem(new ItemStack(Material.TOTEM_OF_UNDYING));
        meta.setBlockState(box);
        shulker.setItemMeta(meta);
        return shulker;
    }
}
