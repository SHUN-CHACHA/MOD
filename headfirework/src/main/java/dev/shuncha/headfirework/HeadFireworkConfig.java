package dev.shuncha.headfirework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.component.FireworkExplosion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class HeadFireworkConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("headfirework.json");

    // デフォルト値(RESET時にこの値に戻る)
    public static final float DEFAULT_SCALE_SMALL_BALL = 5.0f;
    public static final float DEFAULT_SCALE_LARGE_BALL = 10.0f;
    public static final float DEFAULT_SCALE_STAR = 14.0f;
    public static final float DEFAULT_SCALE_CREEPER = 14.0f;
    public static final float DEFAULT_SCALE_BURST = 14.0f;
    public static final int DEFAULT_ANIMATION_DURATION_TICKS = 10;
    public static final float DEFAULT_ANIMATION_START_RATIO = 0.2f;
    public static final int DEFAULT_DISPLAY_DURATION_TICKS = 60;
    public static final int DEFAULT_FADE_DURATION_TICKS = 10;
    // 顔の正面が向く方角(north/south/east/west)
    public static final String DEFAULT_FACING = "south";
    public static final String[] VALID_FACINGS = {"north", "south", "east", "west"};

    public static HeadFireworkConfig INSTANCE = load();

    // 形状ごとのスケール倍率
    public float scaleSmallBall = DEFAULT_SCALE_SMALL_BALL;
    public float scaleLargeBall = DEFAULT_SCALE_LARGE_BALL;
    public float scaleStar = DEFAULT_SCALE_STAR;
    public float scaleCreeper = DEFAULT_SCALE_CREEPER;
    public float scaleBurst = DEFAULT_SCALE_BURST;

    // 拡大アニメーション設定
    public int animationDurationTicks = DEFAULT_ANIMATION_DURATION_TICKS;
    public float animationStartRatio = DEFAULT_ANIMATION_START_RATIO;

    // 表示時間(拡大完了後、フェードアウト開始までの維持時間)
    public int displayDurationTicks = DEFAULT_DISPLAY_DURATION_TICKS;

    // フェードアウト(縮小)にかける時間
    public int fadeDurationTicks = DEFAULT_FADE_DURATION_TICKS;

    // 顔の向き
    public String facing = DEFAULT_FACING;

    public static HeadFireworkConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                HeadFireworkConfig config = GSON.fromJson(json, HeadFireworkConfig.class);
                if (config != null) {
                    if (config.facing == null) {
                        config.facing = DEFAULT_FACING;
                    }
                    return config;
                }
            }
        } catch (IOException e) {
            HeadFireworkMod.LOGGER.warn("Failed to load headfirework config, using defaults", e);
        }
        HeadFireworkConfig config = new HeadFireworkConfig();
        config.save();
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            HeadFireworkMod.LOGGER.warn("Failed to save headfirework config", e);
        }
    }

    public void resetToDefaults() {
        scaleSmallBall = DEFAULT_SCALE_SMALL_BALL;
        scaleLargeBall = DEFAULT_SCALE_LARGE_BALL;
        scaleStar = DEFAULT_SCALE_STAR;
        scaleCreeper = DEFAULT_SCALE_CREEPER;
        scaleBurst = DEFAULT_SCALE_BURST;
        animationDurationTicks = DEFAULT_ANIMATION_DURATION_TICKS;
        animationStartRatio = DEFAULT_ANIMATION_START_RATIO;
        displayDurationTicks = DEFAULT_DISPLAY_DURATION_TICKS;
        fadeDurationTicks = DEFAULT_FADE_DURATION_TICKS;
        facing = DEFAULT_FACING;
        save();
    }

    public float scaleForShape(FireworkExplosion.Shape shape) {
        return switch (shape) {
            case SMALL_BALL -> scaleSmallBall;
            case LARGE_BALL -> scaleLargeBall;
            case STAR -> scaleStar;
            case CREEPER -> scaleCreeper;
            case BURST -> scaleBurst;
            default -> scaleStar;
        };
    }

    // 東西南北をY軸回転角(度)に変換
    public float facingYawDegrees() {
        return switch (facing) {
            case "north" -> 180f;
            case "east" -> -90f;
            case "west" -> 90f;
            default -> 0f; // south
        };
    }
}