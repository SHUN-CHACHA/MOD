package dev.shuncha.headfireworkpaper;

import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.FireworkExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 花火の爆発を検知し、プレイヤーヘッドの持ち主の顔を
 * 拡大→維持→フェードアウトのアニメーションで表示するクラス。
 */
public class FireworkListener implements Listener {

    private final HeadFireworkPaperPlugin plugin;

    public FireworkListener(HeadFireworkPaperPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFireworkExplode(FireworkExplodeEvent event) {
        Firework firework = event.getEntity();
        FireworkMeta meta = firework.getFireworkMeta();

        // FireworkMetaはItemMeta同様PersistentDataHolderであり、ロケットアイテムから
        // 発射されたFireworkエンティティにもPDCの内容が引き継がれる想定。
        String ownersRaw = meta.getPersistentDataContainer()
                .get(plugin.getRecipeManager().getOwnersKey(), PersistentDataType.STRING);
        plugin.getLogger().info("[DEBUG] onFireworkExplode: ownersRaw=" + ownersRaw
                + " effects=" + meta.getEffects().size());
        if (ownersRaw == null || ownersRaw.isBlank()) return;

        String[] ownerNames = ownersRaw.split(",", -1);
        List<FireworkEffect> effects = meta.getEffects();
        plugin.getLogger().info("[DEBUG] ownerNames=" + java.util.Arrays.toString(ownerNames));

        for (int i = 0; i < effects.size() && i < ownerNames.length; i++) {
            plugin.getLogger().info("[DEBUG] spawnHead呼び出し i=" + i + " owner=" + ownerNames[i]
                    + " shape=" + effects.get(i).getType());
            spawnHead(firework.getLocation(), ownerNames[i], effects.get(i).getType(), i, effects.size());
        }
    }

    private void spawnHead(Location baseLocation, String ownerName, FireworkEffect.Type shape, int index, int totalCount) {
        if (ownerName == null || ownerName.isBlank()) {
            plugin.getLogger().info("[DEBUG] spawnHead: ownerNameが空のため中断");
            return;
        }

        HeadFireworkConfig config = plugin.getHeadFireworkConfig();
        HeadFireworkConfig.ExplosionShape configShape = toConfigShape(shape);
        float targetScale = (float) config.getScale(configShape);

        // 複数人分の顔が同じ座標に重なって見えなくなるのを防ぐため、
        // 表示サイズに応じた間隔で横一列に並べる。
        // 「顔が向いている方角(facingYawDegrees)」と同じ軸でずらすと、正面から見たときに
        // 奥行き方向に重なって見えてしまうため、向きに対して垂直な左右方向にずらす。
        double spacing = Math.max(1.0, targetScale * 0.8);
        double offsetAmount = (index - (totalCount - 1) / 2.0) * spacing;
        double yawRad = Math.toRadians(config.facingYawDegrees());
        double perpX = -Math.cos(yawRad);
        double perpZ = Math.sin(yawRad);
        Location location = baseLocation.clone().add(perpX * offsetAmount, 0, perpZ * offsetAmount);

        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
        // 名前ベースでプロフィールを作成する。テクスチャは名前から
        // サーバー/クライアント側で自動的に解決される(バニラの
        // player_head[profile="名前"]と同じ仕組み)。
        PlayerProfile profile = plugin.getServer().createPlayerProfile(ownerName);
        skullMeta.setOwnerProfile(profile);
        headItem.setItemMeta(skullMeta);

        ItemDisplay display = location.getWorld().spawn(location, ItemDisplay.class, d -> {
            d.setItemStack(headItem);
            d.setBillboard(Display.Billboard.FIXED);
            // 変換プリセット(FIXED等)は狙った向きにならなかったため、
            // NONE(無変換)にして自前の回転だけで向きを制御する。
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            // 花火(エンティティ)自体が飛翔中の向き(yaw/pitch)を持っているため、
            // それをリセットしないとTransformationの回転と二重にかかってしまう。
            d.setRotation(0f, 0f);
            d.setTransformation(scaledTransformation(0f, config.facingYawDegrees()));
        });

        int animationDuration = config.getAnimationDuration();
        int displayDuration = config.getDisplayDuration();
        int fadeDuration = config.getFadeDuration();
        int fadeStartTick = Math.max(0, displayDuration - fadeDuration);

        new BukkitRunnable() {
            int tick = 0;

            @Override
            public void run() {
                if (!display.isValid()) {
                    cancel();
                    return;
                }

                float scale;
                if (tick < animationDuration) {
                    // 拡大フェーズ: 0 → targetScale
                    scale = targetScale * (tick / (float) Math.max(1, animationDuration));
                } else if (tick < fadeStartTick) {
                    // 維持フェーズ
                    scale = targetScale;
                } else if (tick < displayDuration) {
                    // フェードアウトフェーズ: targetScale → 0
                    float progress = (tick - fadeStartTick) / (float) Math.max(1, fadeDuration);
                    scale = targetScale * (1f - progress);
                } else {
                    display.remove();
                    cancel();
                    return;
                }

                display.setTransformation(scaledTransformation(Math.max(0f, scale), config.facingYawDegrees()));
                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /**
     * デバッグ用: 再ビルドせずに任意のpitch/roll/yawを試せるように、
     * プレイヤーの前に静止した頭を1つ出す。5秒後に自動で消える。
     */
    public void spawnDebugHead(org.bukkit.entity.Player player, float pitchDeg, float rollDeg, float yawDeg) {
        Location loc = player.getEyeLocation().add(player.getLocation().getDirection().normalize().multiply(3));

        ItemStack headItem = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) headItem.getItemMeta();
        PlayerProfile profile = plugin.getServer().createPlayerProfile(player.getName());
        skullMeta.setOwnerProfile(profile);
        headItem.setItemMeta(skullMeta);

        Quaternionf rotation = new Quaternionf();
        rotation.rotateY((float) Math.toRadians(yawDeg));
        rotation.rotateX((float) Math.toRadians(pitchDeg));
        rotation.rotateY((float) Math.toRadians(rollDeg));
        Transformation transformation = new Transformation(
                new Vector3f(0f, 0f, 0f),
                rotation,
                new Vector3f(8f, 8f, 8f),
                new Quaternionf()
        );

        ItemDisplay display = loc.getWorld().spawn(loc, ItemDisplay.class, d -> {
            d.setItemStack(headItem);
            d.setBillboard(Display.Billboard.FIXED);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.NONE);
            d.setRotation(0f, 0f);
            d.setTransformation(transformation);
        });

        player.sendMessage("pitch=" + pitchDeg + " roll=" + rollDeg + " yaw=" + yawDeg + " で頭を出しました(5秒で消えます)");
        plugin.getServer().getScheduler().runTaskLater(plugin, display::remove, 100L);
    }

    private Transformation scaledTransformation(float scale, float yawDegrees) {
        // 実機検証の結果、頭のモデルは無回転(yaw=0)の時点で既に正しい上下関係になっており、
        // Y軸回転(yaw)だけで方角を制御できることが分かった。
        Quaternionf rotation = new Quaternionf();
        rotation.rotateY((float) Math.toRadians(yawDegrees));
        return new Transformation(
                new Vector3f(0f, 0f, 0f),
                rotation,
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        );
    }

    private HeadFireworkConfig.ExplosionShape toConfigShape(FireworkEffect.Type type) {
        return switch (type) {
            case BALL -> HeadFireworkConfig.ExplosionShape.SMALL_BALL;
            case BALL_LARGE -> HeadFireworkConfig.ExplosionShape.LARGE_BALL;
            case STAR -> HeadFireworkConfig.ExplosionShape.STAR;
            case CREEPER -> HeadFireworkConfig.ExplosionShape.CREEPER;
            case BURST -> HeadFireworkConfig.ExplosionShape.BURST;
        };
    }
}
