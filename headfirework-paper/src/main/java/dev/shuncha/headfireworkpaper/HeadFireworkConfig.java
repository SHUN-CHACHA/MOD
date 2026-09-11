package dev.shuncha.headfireworkpaper;

import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;

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
        plugin.saveConfig();
    }

    private BlockFace parseFacing(String value) {
        if (value == null) return BlockFace.SOUTH;
        return switch (value.toLowerCase()) {
            case "north" -> BlockFace.NORTH;
            case "east" -> BlockFace.EAST;
            case "west" -> BlockFace.WEST;
            default -> BlockFace.SOUTH;
        };
    }

    /** 方角(BlockFace)をY軸回転角(度)に変換する。実機検証済みの値。 */
    public float facingYawDegrees() {
        return switch (facing) {
            case NORTH -> 0f;
            case WEST -> 90f;
            case SOUTH -> 180f;
            default -> 270f; // EAST
        };
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

    /** GUIの「顔の向き」ボタン用: 北→東→南→西と循環させる。 */
    public void cycleFacing() {
        facing = switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            default -> BlockFace.NORTH;
        };
    }
}
