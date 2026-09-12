package dev.shuncha.headfireworkpaper;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.UUID;

/**
 * /headfirework gui(管理者用)と /headfirework mygui(参加者本人用)の
 * チェストGUIをまとめて扱うクラス。
 *
 * Fabric MOD版のCtrl+JキーによるGUI画面(クライアント側MOD前提)の代わりに、
 * サーバー側だけで完結するチェストGUIとして実装している。
 */
public class HeadFireworkGuiListener implements Listener {

    // ------------------------------------------------------------------
    // 管理者用GUI(/headfirework gui) — サーバー全体の設定
    // ------------------------------------------------------------------

    private static final int SIZE = 54;

    // スロット番号の割り当て
    private static final int SMALL_BALL_MINUS = 10, SMALL_BALL_DISPLAY = 11, SMALL_BALL_PLUS = 12, SMALL_BALL_RESET = 13;
    private static final int LARGE_BALL_MINUS = 19, LARGE_BALL_DISPLAY = 20, LARGE_BALL_PLUS = 21, LARGE_BALL_RESET = 22;
    private static final int STAR_BURST_MINUS = 28, STAR_BURST_DISPLAY = 29, STAR_BURST_PLUS = 30, STAR_BURST_RESET = 31;
    private static final int DISPLAY_DURATION_MINUS = 37, DISPLAY_DURATION_DISPLAY = 38, DISPLAY_DURATION_PLUS = 39, DISPLAY_DURATION_RESET = 40;
    private static final int FADE_DURATION_MINUS = 46, FADE_DURATION_DISPLAY = 47, FADE_DURATION_PLUS = 48, FADE_DURATION_RESET = 49;
    private static final int FACING_BUTTON = 4;
    private static final int CLOSE_BUTTON = 53;

    // ------------------------------------------------------------------
    // 参加者本人用GUI(/headfirework mygui) — 自分の顔の向きだけの簡易版
    // ------------------------------------------------------------------

    private static final int PERSONAL_SIZE = 27;
    private static final int PERSONAL_FACING_BUTTON = 13;
    private static final int PERSONAL_RESET_BUTTON = 11;
    private static final int PERSONAL_CLOSE_BUTTON = 26;

    private final HeadFireworkPaperPlugin plugin;

    public HeadFireworkGuiListener(HeadFireworkPaperPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    // 管理者用GUI
    // ------------------------------------------------------------------

    public void open(Player player) {
        Holder holder = new Holder();
        Inventory inventory = plugin.getServer().createInventory(holder, SIZE,
                ChatColor.DARK_GRAY + "HeadFirework 設定");
        holder.inventory = inventory;
        render(inventory);
        player.openInventory(inventory);
    }

    /** 現在の設定値をもとに、GUI内の各アイテムを作り直す。 */
    private void render(Inventory inventory) {
        HeadFireworkConfig config = plugin.getHeadFireworkConfig();
        inventory.clear();

        fillBackground(inventory);

        setScaleRow(inventory, SMALL_BALL_MINUS, SMALL_BALL_DISPLAY, SMALL_BALL_PLUS, SMALL_BALL_RESET,
                "小玉サイズ", HeadFireworkConfig.ExplosionShape.SMALL_BALL, Material.FIRE_CHARGE);
        setScaleRow(inventory, LARGE_BALL_MINUS, LARGE_BALL_DISPLAY, LARGE_BALL_PLUS, LARGE_BALL_RESET,
                "大玉サイズ", HeadFireworkConfig.ExplosionShape.LARGE_BALL, Material.FIRE_CHARGE);
        setScaleRow(inventory, STAR_BURST_MINUS, STAR_BURST_DISPLAY, STAR_BURST_PLUS, STAR_BURST_RESET,
                "星型/バーストサイズ", HeadFireworkConfig.ExplosionShape.STAR, Material.FIREWORK_STAR);

        inventory.setItem(DISPLAY_DURATION_MINUS, arrowItem("-5"));
        inventory.setItem(DISPLAY_DURATION_DISPLAY, valueItem(Material.CLOCK, "表示時間", config.getDisplayDuration() + " tick"));
        inventory.setItem(DISPLAY_DURATION_PLUS, arrowItem("+5"));
        inventory.setItem(DISPLAY_DURATION_RESET, resetItem());

        inventory.setItem(FADE_DURATION_MINUS, arrowItem("-5"));
        inventory.setItem(FADE_DURATION_DISPLAY, valueItem(Material.GLASS, "フェードアウト時間", config.getFadeDuration() + " tick"));
        inventory.setItem(FADE_DURATION_PLUS, arrowItem("+5"));
        inventory.setItem(FADE_DURATION_RESET, resetItem());

        inventory.setItem(FACING_BUTTON, facingItem(config.getFacing(),
                "サーバー全体のデフォルトの向き", "クリックで切り替え(北→東→南→西)"));
        inventory.setItem(CLOSE_BUTTON, closeItem());
    }

    private void setScaleRow(Inventory inventory, int minusSlot, int displaySlot, int plusSlot, int resetSlot,
                              String label, HeadFireworkConfig.ExplosionShape shape, Material displayMaterial) {
        HeadFireworkConfig config = plugin.getHeadFireworkConfig();
        inventory.setItem(minusSlot, arrowItem("-1.0"));
        inventory.setItem(displaySlot, valueItem(displayMaterial, label, String.valueOf(config.getScale(shape))));
        inventory.setItem(plusSlot, arrowItem("+1.0"));
        inventory.setItem(resetSlot, resetItem());
    }

    // ------------------------------------------------------------------
    // 参加者本人用GUI
    // ------------------------------------------------------------------

    public void openPersonal(Player player) {
        PersonalHolder holder = new PersonalHolder(player.getUniqueId());
        Inventory inventory = plugin.getServer().createInventory(holder, PERSONAL_SIZE,
                ChatColor.DARK_GRAY + "HeadFirework 自分の設定");
        holder.inventory = inventory;
        renderPersonal(inventory, player);
        player.openInventory(inventory);
    }

    private void renderPersonal(Inventory inventory, Player player) {
        HeadFireworkConfig config = plugin.getHeadFireworkConfig();
        inventory.clear();
        fillBackground(inventory);

        BlockFace effective = config.getPlayerFacing(player.getName()).orElse(config.getFacing());
        boolean isPersonal = config.getPlayerFacing(player.getName()).isPresent();

        String status = isPersonal
                ? ChatColor.AQUA + "個人設定が有効です"
                : ChatColor.GRAY + "サーバーのデフォルト値を使用中です";
        inventory.setItem(PERSONAL_FACING_BUTTON, facingItem(effective,
                "自分の花火の顔の向き", "クリックで切り替え(北→東→南→西)", status));

        ItemStack resetItem = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = resetItem.getItemMeta();
        resetMeta.setDisplayName(ChatColor.RED + "サーバーのデフォルト値に戻す");
        resetMeta.setLore(List.of(ChatColor.GRAY + "個人設定を削除します"));
        resetItem.setItemMeta(resetMeta);
        inventory.setItem(PERSONAL_RESET_BUTTON, resetItem);

        inventory.setItem(PERSONAL_CLOSE_BUTTON, closeItem());
    }

    // ------------------------------------------------------------------
    // GUIパーツ共通部品
    // ------------------------------------------------------------------

    private void fillBackground(Inventory inventory) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, filler);
        }
    }

    private ItemStack arrowItem(String label) {
        ItemStack item = new ItemStack(label.startsWith("-") ? Material.RED_DYE : Material.LIME_DYE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.YELLOW + label);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack valueItem(Material material, String label, String value) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + label);
        meta.setLore(List.of(ChatColor.GRAY + "現在値: " + ChatColor.AQUA + value));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack resetItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "リセット");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack facingItem(BlockFace facing, String titleLabel, String... loreLines) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + titleLabel + ": " + facingLabel(facing));
        meta.setLore(List.of(loreLines).stream().map(line -> ChatColor.GRAY + line).toList());
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack closeItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "閉じる");
        item.setItemMeta(meta);
        return item;
    }

    private String facingLabel(BlockFace facing) {
        return switch (facing) {
            case NORTH -> "北 (north)";
            case EAST -> "東 (east)";
            case WEST -> "西 (west)";
            default -> "南 (south)";
        };
    }

    // ------------------------------------------------------------------
    // クリック処理
    // ------------------------------------------------------------------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof PersonalHolder personalHolder) {
            handlePersonalClick(event, personalHolder);
            return;
        }
        if (!(event.getInventory().getHolder() instanceof Holder)) return;
        event.setCancelled(true); // アイテムを持ち出せないようにする

        if (!(event.getWhoClicked() instanceof Player player)) return;
        int slot = event.getRawSlot();
        HeadFireworkConfig config = plugin.getHeadFireworkConfig();

        switch (slot) {
            case SMALL_BALL_MINUS -> adjustScale(config, HeadFireworkConfig.ExplosionShape.SMALL_BALL, -1.0);
            case SMALL_BALL_PLUS -> adjustScale(config, HeadFireworkConfig.ExplosionShape.SMALL_BALL, 1.0);
            case SMALL_BALL_RESET -> config.resetScale(HeadFireworkConfig.ExplosionShape.SMALL_BALL);

            case LARGE_BALL_MINUS -> adjustScale(config, HeadFireworkConfig.ExplosionShape.LARGE_BALL, -1.0);
            case LARGE_BALL_PLUS -> adjustScale(config, HeadFireworkConfig.ExplosionShape.LARGE_BALL, 1.0);
            case LARGE_BALL_RESET -> config.resetScale(HeadFireworkConfig.ExplosionShape.LARGE_BALL);

            case STAR_BURST_MINUS -> adjustScale(config, HeadFireworkConfig.ExplosionShape.STAR, -1.0);
            case STAR_BURST_PLUS -> adjustScale(config, HeadFireworkConfig.ExplosionShape.STAR, 1.0);
            case STAR_BURST_RESET -> config.resetScale(HeadFireworkConfig.ExplosionShape.STAR);

            case DISPLAY_DURATION_MINUS -> config.setDisplayDuration(Math.max(0, config.getDisplayDuration() - 5));
            case DISPLAY_DURATION_PLUS -> config.setDisplayDuration(config.getDisplayDuration() + 5);
            case DISPLAY_DURATION_RESET -> config.resetDisplayDuration();

            case FADE_DURATION_MINUS -> config.setFadeDuration(Math.max(0, config.getFadeDuration() - 5));
            case FADE_DURATION_PLUS -> config.setFadeDuration(config.getFadeDuration() + 5);
            case FADE_DURATION_RESET -> config.resetFadeDuration();

            case FACING_BUTTON -> config.cycleFacing();

            case CLOSE_BUTTON -> {
                config.save();
                player.closeInventory();
                return;
            }
            default -> {
                return; // 背景パネル等、意味のないスロットのクリックは何もしない
            }
        }

        config.save();
        render(event.getInventory());
    }

    private void handlePersonalClick(InventoryClickEvent event, PersonalHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        // 本人以外がこのGUIを操作することは想定していないが、念のため確認する。
        if (!player.getUniqueId().equals(holder.ownerId)) return;

        HeadFireworkConfig config = plugin.getHeadFireworkConfig();
        switch (event.getRawSlot()) {
            case PERSONAL_FACING_BUTTON -> {
                BlockFace current = config.getPlayerFacing(player.getName()).orElse(config.getFacing());
                config.setPlayerFacing(player.getName(), config.cycleFacingValue(current));
            }
            case PERSONAL_RESET_BUTTON -> config.resetPlayerFacing(player.getName());
            case PERSONAL_CLOSE_BUTTON -> {
                config.save();
                player.closeInventory();
                return;
            }
            default -> {
                return;
            }
        }

        config.save();
        renderPersonal(event.getInventory(), player);
    }

    private void adjustScale(HeadFireworkConfig config, HeadFireworkConfig.ExplosionShape shape, double delta) {
        double newValue = Math.max(0.5, config.getScale(shape) + delta);
        config.setScale(shape, newValue);
    }

    /** 管理者用GUIのインベントリだと識別するためのマーカー。 */
    private static class Holder implements InventoryHolder {
        private Inventory inventory;

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    /** 参加者本人用GUIのインベントリだと識別するためのマーカー。誰が開いたGUIかも保持する。 */
    private static class PersonalHolder implements InventoryHolder {
        private final UUID ownerId;
        private Inventory inventory;

        private PersonalHolder(UUID ownerId) {
            this.ownerId = ownerId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
