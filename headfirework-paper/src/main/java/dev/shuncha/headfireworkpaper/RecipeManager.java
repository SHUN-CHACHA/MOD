package dev.shuncha.headfireworkpaper;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkEffectMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * プレイヤーヘッド花火の星・ロケットのクラフトを扱うクラス。
 *
 * Bukkitの通常のRecipe(固定材料数)では「染料・頭を複数個まとめて投入」のような
 * 可変個数のレシピを表現できないため、PrepareItemCraftEventでクラフト盤面を
 * 直接検証し、結果アイテムを手動で組み立てる方式を取っている。
 * (Fabric MOD版でいうMixinでのレシピ横取りに相当する処理)
 */
public class RecipeManager implements Listener {

    private final HeadFireworkPaperPlugin plugin;
    private final NamespacedKey ownersKey;

    public RecipeManager(HeadFireworkPaperPlugin plugin) {
        this.plugin = plugin;
        this.ownersKey = new NamespacedKey(plugin, "owners");
    }

    public NamespacedKey getOwnersKey() {
        return ownersKey;
    }

    // ------------------------------------------------------------------
    // 盤面のプレビュー(結果アイテムの計算)
    // ------------------------------------------------------------------

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        ItemStack starResult = tryBuildStar(matrix);
        if (starResult != null) {
            inv.setResult(starResult);
            return;
        }

        ItemStack rocketResult = tryBuildRocket(matrix);
        if (rocketResult != null) {
            inv.setResult(rocketResult);
        }
        // どちらにも該当しない場合はバニラの通常レシピ判定に任せる(何もしない)
    }

    // ------------------------------------------------------------------
    // 星のレシピ判定
    // ------------------------------------------------------------------

    private ItemStack tryBuildStar(ItemStack[] matrix) {
        int gunpowder = 0;
        int fireCharge = 0, feather = 0, goldNugget = 0;
        int diamond = 0, glowstoneDust = 0;
        List<DyeColor> dyeColors = new ArrayList<>();
        String ownerName = null;
        int headCount = 0;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;
            Material type = item.getType();

            if (type == Material.GUNPOWDER) {
                gunpowder += item.getAmount();
            } else if (isDye(type)) {
                DyeColor color = dyeColorOf(type);
                for (int i = 0; i < item.getAmount(); i++) dyeColors.add(color);
            } else if (type == Material.PLAYER_HEAD) {
                String name = ownerNameOf(item);
                if (name == null) return null; // オーナー情報の無い頭は無効
                if (ownerName == null) {
                    ownerName = name;
                } else if (!ownerName.equalsIgnoreCase(name)) {
                    return null; // 異なるプレイヤーの頭が混在 → 無効
                }
                headCount += item.getAmount();
            } else if (type == Material.FIRE_CHARGE) {
                fireCharge += item.getAmount();
            } else if (type == Material.FEATHER) {
                feather += item.getAmount();
            } else if (type == Material.GOLD_NUGGET) {
                goldNugget += item.getAmount();
            } else if (type == Material.DIAMOND) {
                diamond += item.getAmount();
            } else if (type == Material.GLOWSTONE_DUST) {
                glowstoneDust += item.getAmount();
            } else {
                return null; // 想定外のアイテムが混ざっている → 無効
            }
        }

        if (gunpowder != 1 || dyeColors.isEmpty() || headCount == 0) return null;
        int shapeItems = fireCharge + feather + goldNugget;
        if (shapeItems > 1) return null; // 形状材料は同時に1種類まで
        if (diamond > 1 || glowstoneDust > 1) return null;

        FireworkEffect.Type shapeType = FireworkEffect.Type.BALL;
        if (fireCharge == 1) shapeType = FireworkEffect.Type.BALL_LARGE;
        else if (feather == 1) shapeType = FireworkEffect.Type.STAR;
        else if (goldNugget == 1) shapeType = FireworkEffect.Type.BURST;

        List<Color> colors = new ArrayList<>();
        for (DyeColor dc : dyeColors) colors.add(dc.getFireworkColor());

        FireworkEffect effect = FireworkEffect.builder()
                .with(shapeType)
                .withColor(colors)
                .flicker(diamond == 1)
                .trail(glowstoneDust == 1)
                .build();

        ItemStack result = new ItemStack(Material.FIREWORK_STAR, 1);
        FireworkEffectMeta meta = (FireworkEffectMeta) result.getItemMeta();
        meta.setEffect(effect);
        meta.setDisplayName(ownerName + "花火の星");
        meta.getPersistentDataContainer().set(ownersKey, PersistentDataType.STRING, ownerName);
        result.setItemMeta(meta);
        return result;
    }

    // ------------------------------------------------------------------
    // ロケットのレシピ判定
    // ------------------------------------------------------------------

    private ItemStack tryBuildRocket(ItemStack[] matrix) {
        int paper = 0;
        int gunpowder = 0;
        List<FireworkEffect> effects = new ArrayList<>();
        // ownersPerEffect は effects と同じ並び順・同じ添字で対応する
        // (「何番目の爆発エフェクトが誰の顔か」を爆発時に引くための対応表)
        List<String> ownersPerEffect = new ArrayList<>();
        int starCount = 0;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;
            Material type = item.getType();

            if (type == Material.PAPER) {
                paper += item.getAmount();
            } else if (type == Material.GUNPOWDER) {
                gunpowder += item.getAmount();
            } else if (type == Material.FIREWORK_STAR && isOurStar(item)) {
                FireworkEffectMeta meta = (FireworkEffectMeta) item.getItemMeta();
                String owner = meta.getPersistentDataContainer().get(ownersKey, PersistentDataType.STRING);
                for (int i = 0; i < item.getAmount(); i++) {
                    effects.add(meta.getEffect());
                    ownersPerEffect.add(owner);
                    starCount++;
                }
            } else if (type == Material.FIREWORK_STAR) {
                plugin.getLogger().info("[DEBUG] tryBuildRocket: FIREWORK_STARだがisOurStar()がfalse → 無効化 (displayName="
                        + (item.getItemMeta() != null ? item.getItemMeta().getDisplayName() : "null") + ")");
                return null;
            } else {
                plugin.getLogger().info("[DEBUG] tryBuildRocket: 想定外のアイテム(" + type + ")が混在 → 無効化");
                return null;
            }
        }

        if (starCount == 0) return null;
        if (paper != starCount) {
            plugin.getLogger().info("[DEBUG] tryBuildRocket: 紙の数(" + paper + ")と星の数(" + starCount + ")が不一致 → 無効化");
            return null;
        }
        if (gunpowder < 1 || gunpowder > 3) {
            plugin.getLogger().info("[DEBUG] tryBuildRocket: 火薬の数(" + gunpowder + ")が範囲外 → 無効化");
            return null;
        }

        ItemStack result = new ItemStack(Material.FIREWORK_ROCKET, 3 * starCount);
        FireworkMeta meta = (FireworkMeta) result.getItemMeta();
        meta.addEffects(effects);
        meta.setPower(gunpowder);
        // 重複を除いたオーナー名一覧を表示名に使う(例: 1人なら「Steve花火」、複数人なら連名)
        java.util.LinkedHashSet<String> uniqueOwners = new java.util.LinkedHashSet<>(ownersPerEffect);
        meta.setDisplayName(String.join("・", uniqueOwners) + "花火");
        // 「,」区切りでeffectsと同じ並び順のownerリストを保存する(効果索引→ownerの対応を保つため)
        meta.getPersistentDataContainer().set(ownersKey, PersistentDataType.STRING, String.join(",", ownersPerEffect));
        result.setItemMeta(meta);
        return result;
    }

    // ------------------------------------------------------------------
    // クラフト実行時の消費制御(頭は消費しない)
    // ------------------------------------------------------------------

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (result == null) return;
        if (!isOurStar(result) && !isOurRocket(result)) return;

        // 星クラフトの場合のみ、盤面中のプレイヤーヘッドを消費しないよう戻す。
        // (ロケットクラフトでは頭は使わないため対象外)
        if (!isOurStar(result)) return;

        CraftingInventory inv = event.getInventory();
        // 1tick後(バニラの消費処理が終わった後)に頭を復元する。
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack[] matrix = inv.getMatrix();
            boolean changed = false;
            for (int i = 0; i < matrix.length; i++) {
                ItemStack item = matrix[i];
                if (item != null && item.getType() == Material.PLAYER_HEAD) {
                    // 消費前の個数はここでは分からないため、単純に1個分を戻す。
                    // (バニラのgetRemainingItemsに相当する処理を簡易的に再現)
                    item.setAmount(item.getAmount() + 1);
                    matrix[i] = item;
                    changed = true;
                }
            }
            if (changed) {
                inv.setMatrix(matrix);
            }
        });
    }

    // ------------------------------------------------------------------
    // 判定用ユーティリティ
    // ------------------------------------------------------------------

    public boolean isOurStar(ItemStack item) {
        if (item == null || item.getType() != Material.FIREWORK_STAR) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(ownersKey, PersistentDataType.STRING);
    }

    public boolean isOurRocket(ItemStack item) {
        if (item == null || item.getType() != Material.FIREWORK_ROCKET) return false;
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(ownersKey, PersistentDataType.STRING);
    }

    private boolean isDye(Material type) {
        return type.name().endsWith("_DYE");
    }

    private DyeColor dyeColorOf(Material type) {
        String name = type.name().replace("_DYE", "");
        return DyeColor.valueOf(name);
    }

    /**
     * ヘッドアイテムのオーナー名を取得する。
     * UUIDでの厳密な照合はせず、プロフィールに含まれる名前をそのまま使う方式にしている。
     * (update()でのMojang問い合わせをメインスレッドで同期的に待つと、Bukkitの仕様上
     *  ブロッキングの問題が起きるため、名前ベースの単純な方式に変更した)
     */
    private String ownerNameOf(ItemStack head) {
        ItemMeta meta = head.getItemMeta();
        if (!(meta instanceof SkullMeta skullMeta)) return null;
        PlayerProfile profile = skullMeta.getOwnerProfile();
        if (profile == null) return null;
        return profile.getName();
    }
}
