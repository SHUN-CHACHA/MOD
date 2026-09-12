package dev.shuncha.headfireworkpaper;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * HeadFireworkの各種設定値を保持するクラス。
 * config.ymlへの読み書きを担当する。
 */
public class HeadFireworkConfig {

    public enum ExplosionShape {
        SMALL_BALL, LARGE_BALL, STAR, CREEPER, BURST
    }

    // デフォルト値(Fabric MOD版のデフォルトに合わせる)
    private static final double DEFAULT_SMALL_BALL = 5.0;
    private static final double DEFAULT_LARGE_BALL = 10.0;
    private static final double DEFAULT_STAR_BURST = 14.0;
    private static final int DEFAULT_DISPLAY_DURATION = 60;
    private static final int DEFAULT_ANIMATION_DURATION = 10;
    private static final int DEFAULT_FADE_DURATION = 10;
    private static final String DEFAULT_FACING = "south";

    private final HeadFireworkPaperPlugin plugin;

    private double smallBallScale = DEFAULT_SMALL_BALL;
    private double largeBallScale = DEFAULT_LARGE_BALL;
    private double starBurstScale = DEFAULT_STAR_BURST;
    private int displayDuration = DEFAULT_DISPLAY_DURATION;
    private int animationDuration = DEFAULT_ANIMATION_DURATION;
    private int fadeDuration = DEFAULT_FADE_DURATION;
    private BlockFace facing = BlockFace.SOUTH;

    /**
     * プレイヤーごとの個人設定。現状は「顔の向き」だけだが、将来的にサイズ等の
     * 個人設定を増やしても対応しやすいよう、プレイヤー名をキーにしたMapで持たせている。
     * キーはプレイヤー名(小文字化して比較)。値は設定済みの向きのみを保持し、
     * 未設定のプレイヤーはMapに存在しない(=サーバー全体のデフォルトを使う)。
     */
    private final Map<String, BlockFace> playerFacing = new HashMap<>();

    public HeadFireworkConfig(HeadFireworkPaperPlugin plugin) {
        this.plugin = plugin;
    }

    /** config.ymlから設定を読み込む。存在しない項目はデフォルト値のまま。 */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        smallBallScale = c.getDouble("scale.small_ball", DEFAULT_SMALL_BALL);
        largeBallScale = c.getDouble("scale.large_ball", DEFAULT_LARGE_BALL);
        starBurstScale = c.getDouble("scale.star_burst", DEFAULT_STAR_BURST);
        displayDuration = c.getInt("display_duration", DEFAULT_DISPLAY_DURATION);
        animationDuration = c.getInt("animation_duration", DEFAULT_ANIMATION_DURATION);
        fadeDuration = c.getInt("fade_duration", DEFAULT_FADE_DURATION);
        facing = parseFacing(c.getString("facing", DEFAULT_FACING));

        playerFacing.clear();
        ConfigurationSection playerSection = c.getConfigurationSection("player_settings");
        if (playerSection != null) {
            for (String name : playerSection.getKeys(false)) {
                BlockFace personal = parseFacingOrNull(playerSection.getString(name + ".facing"));
                if (personal != null) {
                    playerFacing.put(name.toLowerCase(), personal);
                }
            }
        }
    }

    /** 現在の設定値をconfig.ymlに保存する。 */
    public void save() {
        FileConfiguration c = plugin.getConfig();
        c.set("scale.small_ball", smallBallScale);
        c.set("scale.large_ball", largeBallScale);
        c.set("scale.star_burst", starBurstScale);
        c.set("display_duration", displayDuration);
        c.set("animation_duration", animationDuration);
        c.set("fade_duration", fadeDuration);
        c.set("facing", facing.name().toLowerCase());

        // プレイヤー個人設定は、削除(reset)されたプレイヤーが残らないよう
        // 一度セクションごとクリアしてから書き直す。
        c.set("player_settings", null);
        for (Map.Entry<String, BlockFace> entry : playerFacing.entrySet()) {
            c.set("player_settings." + entry.getKey() + ".facing", entry.getValue().name().toLowerCase());
        }

        plugin.saveConfig();
    }

    private BlockFace parseFacing(String value) {
        BlockFace face = parseFacingOrNull(value);
        return face != null ? face : BlockFace.SOUTH;
    }

    /** 不正な値やnullの場合にSOUTHへフォールバックせず、そのままnullを返す版。個人設定の読み込み用。 */
    private BlockFace parseFacingOrNull(String value) {
        if (value == null) return null;
        return switch (value.toLowerCase()) {
            case "north" -> BlockFace.NORTH;
            case "east" -> BlockFace.EAST;
            case "south" -> BlockFace.SOUTH;
            case "west" -> BlockFace.WEST;
            default -> null;
        };
    }

    /** 方角(BlockFace)をY軸回転角(度)に変換する。実機検証済みの値。 */
    public float facingYawDegrees() {
        return yawDegreesOf(facing);
    }

    private float yawDegreesOf(BlockFace face) {
        return switch (face) {
            case NORTH -> 0f;
            case WEST -> 90f;
            case SOUTH -> 180f;
            default -> 270f; // EAST
        };
    }

    /**
     * 指定したプレイヤー(花火の持ち主)の顔が実際に向くべきYaw角度を返す。
     * 個人設定(/headfirework myface)があればそれを優先し、無ければサーバー全体の
     * デフォルト設定を使う。花火を作った時点の設定は保存されず、爆発した瞬間の
     * "現在の"設定を都度参照する(現在設定方式)。
     */
    public float facingYawDegreesFor(String playerName) {
        BlockFace personal = playerName != null ? playerFacing.get(playerName.toLowerCase()) : null;
        return yawDegreesOf(personal != null ? personal : facing);
    }

    /** プレイヤー個人の顔の向き設定を取得する。設定していなければ空。 */
    public Optional<BlockFace> getPlayerFacing(String playerName) {
        return Optional.ofNullable(playerFacing.get(playerName.toLowerCase()));
    }

    /** プレイヤー個人の顔の向きを設定する。 */
    public void setPlayerFacing(String playerName, BlockFace face) {
        playerFacing.put(playerName.toLowerCase(), face);
    }

    /** プレイヤー個人の顔の向き設定を削除し、サーバー全体のデフォルトに戻す。 */
    public void resetPlayerFacing(String playerName) {
        playerFacing.remove(playerName.toLowerCase());
    }

    public double getScale(ExplosionShape shape) {
        return switch (shape) {
            case SMALL_BALL -> smallBallScale;
            case LARGE_BALL -> largeBallScale;
            case STAR, BURST, CREEPER -> starBurstScale;
        };
    }

    public void setScale(ExplosionShape shape, double value) {
        switch (shape) {
            case SMALL_BALL -> smallBallScale = value;
            case LARGE_BALL -> largeBallScale = value;
            case STAR, BURST, CREEPER -> starBurstScale = value;
        }
    }

    public void resetScale(ExplosionShape shape) {
        switch (shape) {
            case SMALL_BALL -> smallBallScale = DEFAULT_SMALL_BALL;
            case LARGE_BALL -> largeBallScale = DEFAULT_LARGE_BALL;
            case STAR, BURST, CREEPER -> starBurstScale = DEFAULT_STAR_BURST;
        }
    }

    public int getDisplayDuration() { return displayDuration; }
    public void setDisplayDuration(int v) { displayDuration = v; }
    public void resetDisplayDuration() { displayDuration = DEFAULT_DISPLAY_DURATION; }

    public int getAnimationDuration() { return animationDuration; }
    public void setAnimationDuration(int v) { animationDuration = v; }
    public void resetAnimationDuration() { animationDuration = DEFAULT_ANIMATION_DURATION; }

    public int getFadeDuration() { return fadeDuration; }
    public void setFadeDuration(int v) { fadeDuration = v; }
    public void resetFadeDuration() { fadeDuration = DEFAULT_FADE_DURATION; }

    public BlockFace getFacing() { return facing; }
    public void setFacing(BlockFace f) { facing = f; }

    /** GUIの「顔の向き」ボタン用: 北→東→南→西と循環させる(サーバー全体のデフォルト設定を変更)。 */
    public void cycleFacing() {
        facing = cycleFacingValue(facing);
    }

    /** 北→東→南→西と循環させた次の値を返す(個人設定GUIとの共用ロジック)。 */
    public BlockFace cycleFacingValue(BlockFace current) {
        return switch (current) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }
}
