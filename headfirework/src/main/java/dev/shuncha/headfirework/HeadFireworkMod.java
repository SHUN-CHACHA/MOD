package dev.shuncha.headfirework;

import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.math.Transformation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HeadFireworkMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("headfirework");
    public static final String MOD_ID = "headfirework";

    private static final Map<Integer, FireworkRocketEntity> watchedRockets = new ConcurrentHashMap<>();
    private static final Map<UUID, HeadAnimation> animatingHeads = new ConcurrentHashMap<>();

    // 拡大(grow) → 維持(hold) → 縮小フェードアウト(fade) → 消滅、を1本のタイムラインで管理する
    private record HeadAnimation(Display.ItemDisplay display, ServerLevel serverLevel, ItemStack headStack,
                                   double posX, double posY, double posZ,
                                   float targetScale, float yawDegrees, int startTick,
                                   int growDurationTicks, int holdDurationTicks, int fadeDurationTicks) {

        int fadeStartTick() {
            return startTick + growDurationTicks + holdDurationTicks;
        }

        int endTick() {
            return fadeStartTick() + fadeDurationTicks;
        }
    }

    @Override
    public void onInitialize() {
        LOGGER.info("HeadFirework (26.2) skeleton loaded.");

        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "crafting_player_head_firework_star"),
                PlayerHeadFireworkStarRecipe.SERIALIZER
        );

        Registry.register(
                BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(MOD_ID, "crafting_player_head_firework_rocket"),
                PlayerHeadFireworkRocketRecipe.SERIALIZER
        );

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof FireworkRocketEntity rocket) {
                watchedRockets.put(rocket.getId(), rocket);
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::onServerTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private void registerCommands(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("headfirework")
                .then(Commands.literal("config")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.literal("scale")
                                .then(Commands.argument("shape", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                new String[]{"small_ball", "large_ball", "star", "creeper", "burst"}, builder))
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.1f, 50.0f))
                                                .executes(ctx -> {
                                                    String shapeArg = StringArgumentType.getString(ctx, "shape");
                                                    float value = FloatArgumentType.getFloat(ctx, "value");
                                                    HeadFireworkConfig cfg = HeadFireworkConfig.INSTANCE;
                                                    switch (shapeArg) {
                                                        case "small_ball" -> cfg.scaleSmallBall = value;
                                                        case "large_ball" -> cfg.scaleLargeBall = value;
                                                        case "star" -> cfg.scaleStar = value;
                                                        case "creeper" -> cfg.scaleCreeper = value;
                                                        case "burst" -> cfg.scaleBurst = value;
                                                        default -> {
                                                            ctx.getSource().sendFailure(Component.literal("不明な形状: " + shapeArg));
                                                            return 0;
                                                        }
                                                    }
                                                    cfg.save();
                                                    ctx.getSource().sendSuccess(() ->
                                                            Component.literal(shapeArg + " のサイズを " + value + " に変更しました"), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("display_duration")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1, 1200))
                                        .executes(ctx -> {
                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                            HeadFireworkConfig.INSTANCE.displayDurationTicks = ticks;
                                            HeadFireworkConfig.INSTANCE.save();
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("表示時間を " + ticks + " tick に変更しました"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("animation_duration")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                        .executes(ctx -> {
                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                            HeadFireworkConfig.INSTANCE.animationDurationTicks = ticks;
                                            HeadFireworkConfig.INSTANCE.save();
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("アニメーション時間を " + ticks + " tick に変更しました"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("fade_duration")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(0, 200))
                                        .executes(ctx -> {
                                            int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
                                            HeadFireworkConfig.INSTANCE.fadeDurationTicks = ticks;
                                            HeadFireworkConfig.INSTANCE.save();
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("フェードアウト時間を " + ticks + " tick に変更しました"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("facing")
                                .then(Commands.argument("direction", StringArgumentType.word())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                                HeadFireworkConfig.VALID_FACINGS, builder))
                                        .executes(ctx -> {
                                            String direction = StringArgumentType.getString(ctx, "direction");
                                            boolean valid = false;
                                            for (String f : HeadFireworkConfig.VALID_FACINGS) {
                                                if (f.equals(direction)) {
                                                    valid = true;
                                                    break;
                                                }
                                            }
                                            if (!valid) {
                                                ctx.getSource().sendFailure(Component.literal(
                                                        "不明な方角: " + direction + " (north/south/east/west)"));
                                                return 0;
                                            }
                                            HeadFireworkConfig.INSTANCE.facing = direction;
                                            HeadFireworkConfig.INSTANCE.save();
                                            ctx.getSource().sendSuccess(() ->
                                                    Component.literal("顔の向きを " + direction + " に変更しました"), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("show")
                                .executes(ctx -> {
                                    HeadFireworkConfig cfg = HeadFireworkConfig.INSTANCE;
                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                            "small_ball=%.1f large_ball=%.1f star=%.1f creeper=%.1f burst=%.1f display=%d fade=%d anim=%d facing=%s"
                                                    .formatted(cfg.scaleSmallBall, cfg.scaleLargeBall, cfg.scaleStar,
                                                            cfg.scaleCreeper, cfg.scaleBurst,
                                                            cfg.displayDurationTicks, cfg.fadeDurationTicks,
                                                            cfg.animationDurationTicks, cfg.facing)), false);
                                    return 1;
                                }))));
    }

    private void onServerTick(MinecraftServer server) {
        if (!watchedRockets.isEmpty()) {
            Iterator<Map.Entry<Integer, FireworkRocketEntity>> it = watchedRockets.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, FireworkRocketEntity> entry = it.next();
                FireworkRocketEntity rocket = entry.getValue();
                if (rocket.isRemoved()) {
                    onFireworkExploded(rocket, server);
                    it.remove();
                }
            }
        }

        if (!animatingHeads.isEmpty()) {
            int currentTick = server.getTickCount();
            Iterator<Map.Entry<UUID, HeadAnimation>> it = animatingHeads.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, HeadAnimation> entry = it.next();
                HeadAnimation anim = entry.getValue();

                if (anim.display().isRemoved()) {
                    it.remove();
                    continue;
                }

                int elapsed = currentTick - anim.startTick();
                float scale;
                boolean finished = false;

                if (elapsed < anim.growDurationTicks()) {
                    // 拡大フェーズ
                    if (anim.growDurationTicks() <= 0) {
                        scale = anim.targetScale();
                    } else {
                        float progress = (float) elapsed / anim.growDurationTicks();
                        float startScale = anim.targetScale() * HeadFireworkConfig.INSTANCE.animationStartRatio;
                        scale = Mth.lerp(progress, startScale, anim.targetScale());
                    }
                } else if (currentTick < anim.fadeStartTick()) {
                    // 維持(hold)フェーズ
                    scale = anim.targetScale();
                } else if (currentTick < anim.endTick()) {
                    // 縮小フェードアウトフェーズ
                    int fadeElapsed = currentTick - anim.fadeStartTick();
                    if (anim.fadeDurationTicks() <= 0) {
                        scale = 0f;
                    } else {
                        float progress = (float) fadeElapsed / anim.fadeDurationTicks();
                        scale = Mth.lerp(progress, anim.targetScale(), 0f);
                    }
                } else {
                    // 消滅
                    scale = 0f;
                    finished = true;
                }

                // load()は座標・アイテムも巻き戻すため、毎tick全て再適用する
                setHeadTransformation(anim.display(), anim.serverLevel(), scale, anim.yawDegrees());
                anim.display().setPos(anim.posX(), anim.posY(), anim.posZ());
                anim.display().getSlot(0).set(anim.headStack());

                if (finished) {
                    anim.display().discard();
                    it.remove();
                }
            }
        }
    }

    private static void setHeadTransformation(Display.ItemDisplay display, ServerLevel serverLevel, float scale, float yawDegrees) {
        Quaternionf leftRotation = new Quaternionf().rotateY((float) Math.toRadians(yawDegrees));
        Transformation transformation = new Transformation(
                new Vector3f(0f, 0f, 0f),
                leftRotation,
                new Vector3f(scale, scale, scale),
                new Quaternionf()
        );
        CompoundTag transformTag = (CompoundTag) Transformation.EXTENDED_CODEC
                .encodeStart(NbtOps.INSTANCE, transformation)
                .getOrThrow();
        CompoundTag rootTag = new CompoundTag();
        rootTag.put("transformation", transformTag);
        ValueInput input = TagValueInput.create(ProblemReporter.DISCARDING, serverLevel.registryAccess(), rootTag);
        display.load(input);
    }

    private void onFireworkExploded(FireworkRocketEntity rocket, MinecraftServer server) {
        ItemStack fireworkStack = rocket.getItem();

        // v1.3.0で複数プレイヤー対応: CUSTOM_DATAに保存された複数オーナーのリストを試みる。
        // 無ければ(古い単一オーナー形式)、従来通りPROFILEコンポーネント1つだけで表示する。
        List<ResolvableProfile> owners = PlayerHeadFireworkRocketRecipe.readOwners(fireworkStack);
        if (owners == null || owners.isEmpty()) {
            ResolvableProfile single = fireworkStack.get(DataComponents.PROFILE);
            if (single == null) {
                return;
            }
            owners = List.of(single);
        }

        Fireworks fireworks = fireworkStack.get(DataComponents.FIREWORKS);
        List<FireworkExplosion> explosions = fireworks != null ? fireworks.explosions() : List.of();

        Level level = rocket.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        HeadFireworkConfig cfg = HeadFireworkConfig.INSTANCE;
        float yawDegrees = cfg.facingYawDegrees();
        double yawRad = Math.toRadians(yawDegrees);
        // 顔が向いている方角に対して垂直な左右方向の単位ベクトル(複数人を横に並べるため)
        double perpX = -Math.cos(yawRad);
        double perpZ = Math.sin(yawRad);

        int count = Math.min(owners.size(), explosions.isEmpty() ? owners.size() : explosions.size());
        for (int i = 0; i < count; i++) {
            ResolvableProfile profile = owners.get(i);
            if (profile == null) {
                continue;
            }
            FireworkExplosion.Shape shape = i < explosions.size()
                    ? explosions.get(i).shape()
                    : FireworkExplosion.Shape.SMALL_BALL;
            spawnHead(rocket, server, serverLevel, cfg, profile, shape, i, count, perpX, perpZ, yawDegrees);
        }
    }

    private void spawnHead(FireworkRocketEntity rocket, MinecraftServer server, ServerLevel serverLevel,
                            HeadFireworkConfig cfg, ResolvableProfile profile, FireworkExplosion.Shape shape,
                            int index, int totalCount, double perpX, double perpZ, float yawDegrees) {
        Display.ItemDisplay display = EntityTypes.ITEM_DISPLAY.create(serverLevel, EntitySpawnReason.TRIGGERED);
        if (display == null) {
            LOGGER.warn("HeadFirework: failed to create ItemDisplay entity.");
            return;
        }

        float targetScale = cfg.scaleForShape(shape);

        // 複数人分の顔が同じ座標に重なって見えなくなるのを防ぐため、
        // 表示サイズに応じた間隔で、向いている方角に対して垂直な左右方向に並べる
        double spacing = Math.max(1.0, targetScale * 0.8);
        double offsetAmount = (index - (totalCount - 1) / 2.0) * spacing;
        double posX = rocket.getX() + perpX * offsetAmount;
        double posY = rocket.getY();
        double posZ = rocket.getZ() + perpZ * offsetAmount;

        LOGGER.info("HeadFirework: shape={}, targetScale={}, facing={}", shape, targetScale, cfg.facing);
        float startScale = targetScale * cfg.animationStartRatio;

        // load()が内部で座標・アイテムをリセットするため、先にtransformationを適用する(開始サイズで)
        setHeadTransformation(display, serverLevel, startScale, yawDegrees);

        // load()の後にsetPos()を呼ぶことで、正しい座標を確実に反映させる
        display.setPos(posX, posY, posZ);

        ItemStack headStack = new ItemStack(Items.PLAYER_HEAD);
        headStack.set(DataComponents.PROFILE, profile);
        display.getSlot(0).set(headStack);

        serverLevel.addFreshEntity(display);

        animatingHeads.put(display.getUUID(),
                new HeadAnimation(display, serverLevel, headStack, posX, posY, posZ,
                        targetScale, yawDegrees, server.getTickCount(),
                        cfg.animationDurationTicks, cfg.displayDurationTicks, cfg.fadeDurationTicks));

        LOGGER.info("HeadFirework: displaying head at {}, {}, {}", posX, posY, posZ);
    }
}