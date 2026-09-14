package dev.shuncha.headfirework;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.component.FireworkExplosion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

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

    // 顔の向き(サーバー全体のデフォルト。OPが /headfirework config facing で変更する)
    public String facing = DEFAULT_FACING;

    /**
     * プレイヤーごとの個人向き設定(/headfirework myface で本人が設定)。
     * キーはプレイヤー名を小文字化したもの。未設定のプレイヤーはMapに存在せず、
     * その場合はサーバー全体のデフォルト(facing)を使う。
     * 花火を作った時点の設定は保存せず、爆発した瞬間の"今の"設定を都度参照する(現在設定方式)。
     */
    public Map<String, String> playerFacing = new HashMap<>();

    /**
     * 管理者用の強制モード(/headfirework config force_facing)。
     * nullなら通常通り(個人設定→サーバーデフォルトの順で優先)。
     * north/south/east/westのいずれかが入っている間は、個人設定があっても無視して
     * 全員この向きに統一する(個人設定のデータ自体は消さない。強制モード解除で元に戻る)。
     */
    public String forceFacing = null;

    public static HeadFireworkConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                String json = Files.readString(CONFIG_PATH);
                HeadFireworkConfig config = GSON.fromJson(json, HeadFireworkConfig.class);
                if (config != null) {
                    if (config.facing == null) {
                        config.facing = DEFAULT_FACING;
                    }
                    if (config.playerFacing == null) {
                        // 個人向き設定機能を追加する前の古い設定ファイルには存在しないため補完する
                        config.playerFacing = new HashMap<>();
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
        // プレイヤー個人の向き設定は、この一括リセットの対象にはしない(他人のデータのため)
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

    // 東西南北をY軸回転角(度)に変換(サーバー全体のデフォルト向き用。強制モード中はそちらを優先)
    public float facingYawDegrees() {
        return facingYawDegreesOf(forceFacing != null ? forceFacing : facing);
    }

    /**
     * 指定したプレイヤー(花火の頭の持ち主)の顔が実際に向くべきYaw角度を返す。
     * 強制モード(forceFacing)が有効な間は、個人設定があっても無視してそちらを最優先する。
     * 強制モードが無効なら、個人設定(/headfirework myface)があればそれを優先し、
     * 無ければサーバー全体のデフォルト設定(facing)を使う。
     */
    public float facingYawDegreesFor(String playerName) {
        if (forceFacing != null) {
            return facingYawDegreesOf(forceFacing);
        }
        String personal = getPlayerFacing(playerName);
        return facingYawDegreesOf(personal != null ? personal : facing);
    }

    /** 管理者用の強制モードを有効にする。個人設定を無視して全員この向きに統一する。 */
    public void setForceFacing(String direction) {
        forceFacing = direction;
    }

    /** 管理者用の強制モードを解除する。以後は個人設定(未設定ならサーバーデフォルト)に戻る。 */
    public void clearForceFacing() {
        forceFacing = null;
    }

    public boolean isForceFacingEnabled() {
        return forceFacing != null;
    }

    private float facingYawDegreesOf(String direction) {
        return switch (direction) {
            case "north" -> 180f;
            case "east" -> -90f;
            case "west" -> 90f;
            default -> 0f; // south
        };
    }

    /** プレイヤー個人の顔の向き設定を取得する。設定していなければnull。 */
    public String getPlayerFacing(String playerName) {
        if (playerName == null) {
            return null;
        }
        return playerFacing.get(playerName.toLowerCase());
    }

    /** プレイヤー個人の顔の向きを設定する。directionはvalidateFacingで検証済みの値を渡すこと。 */
    public void setPlayerFacing(String playerName, String direction) {
        if (playerName == null) {
            return;
        }
        playerFacing.put(playerName.toLowerCase(), direction);
    }

    /** プレイヤー個人の顔の向き設定を削除し、サーバー全体のデフォルトに戻す。 */
    public void resetPlayerFacing(String playerName) {
        if (playerName == null) {
            return;
        }
        playerFacing.remove(playerName.toLowerCase());
    }

    /** GUIのボタン用: 北→東→南→西と循環させた次の値を返す。 */
    public String cycleFacingValue(String current) {
        return switch (current) {
            case "north" -> "east";
            case "east" -> "south";
            case "south" -> "west";
            default -> "north"; // west -> north
        };
    }
}
