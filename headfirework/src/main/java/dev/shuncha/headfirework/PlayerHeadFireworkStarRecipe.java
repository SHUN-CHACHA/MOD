package dev.shuncha.headfirework;

import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Objects;

public class PlayerHeadFireworkStarRecipe extends CustomRecipe {

    public static final MapCodec<PlayerHeadFireworkStarRecipe> MAP_CODEC =
            MapCodec.unit(PlayerHeadFireworkStarRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerHeadFireworkStarRecipe> STREAM_CODEC =
            StreamCodec.unit(new PlayerHeadFireworkStarRecipe());
    public static final RecipeSerializer<PlayerHeadFireworkStarRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int gunpowder = 0;
        int head = 0;
        int dye = 0;
        int fireCharge = 0;
        int feather = 0;
        int goldNugget = 0;
        int glowstoneDust = 0;
        int diamond = 0;
        int other = 0;
        String headName = null;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Items.GUNPOWDER)) {
                gunpowder += stack.getCount();
            } else if (stack.is(Items.PLAYER_HEAD) && stack.has(DataComponents.PROFILE)) {
                ResolvableProfile profile = stack.get(DataComponents.PROFILE);
                String name = profileName(profile);
                if (headName == null) {
                    headName = name;
                } else if (!Objects.equals(headName, name)) {
                    // 違うプレイヤーの頭が混ざっている場合はレシピ不成立
                    return false;
                }
                head += stack.getCount();
            } else if (stack.has(DataComponents.DYE)) {
                dye += stack.getCount();
            } else if (stack.is(Items.FIRE_CHARGE)) {
                fireCharge += stack.getCount();
            } else if (stack.is(Items.FEATHER)) {
                feather += stack.getCount();
            } else if (stack.is(Items.GOLD_NUGGET)) {
                goldNugget += stack.getCount();
            } else if (stack.is(Items.GLOWSTONE_DUST)) {
                glowstoneDust += stack.getCount();
            } else if (stack.is(Items.DIAMOND)) {
                diamond += stack.getCount();
            } else {
                other++;
            }
        }

        int shapeItems = fireCharge + feather + goldNugget;

        return gunpowder == 1
                && head >= 1
                && headName != null
                && dye >= 1 && dye <= 8
                && shapeItems <= 1
                && glowstoneDust <= 1
                && diamond <= 1
                && other == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack headStack = ItemStack.EMPTY;
        IntList colors = new IntArrayList();
        boolean hasFireCharge = false;
        boolean hasFeather = false;
        boolean hasGoldNugget = false;
        boolean hasTrail = false;
        boolean hasTwinkle = false;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Items.PLAYER_HEAD)) {
                if (headStack.isEmpty()) {
                    headStack = stack;
                }
            } else if (stack.has(DataComponents.DYE)) {
                DyeColor color = stack.get(DataComponents.DYE);
                if (color != null) {
                    for (int i = 0; i < stack.getCount(); i++) {
                        colors.add(color.getFireworkColor());
                    }
                }
            } else if (stack.is(Items.FIRE_CHARGE)) {
                hasFireCharge = true;
            } else if (stack.is(Items.FEATHER)) {
                hasFeather = true;
            } else if (stack.is(Items.GOLD_NUGGET)) {
                hasGoldNugget = true;
            } else if (stack.is(Items.GLOWSTONE_DUST)) {
                hasTrail = true;
            } else if (stack.is(Items.DIAMOND)) {
                hasTwinkle = true;
            }
        }

        FireworkExplosion.Shape shape;
        if (hasFireCharge) {
            shape = FireworkExplosion.Shape.LARGE_BALL;
        } else if (hasFeather) {
            shape = FireworkExplosion.Shape.STAR;
        } else if (hasGoldNugget) {
            shape = FireworkExplosion.Shape.BURST;
        } else {
            shape = FireworkExplosion.Shape.SMALL_BALL;
        }

        // 頭は何個入れても(何個でも消費されず)星は常に1個だけ生成する(Paper版の挙動と統一)
        ItemStack result = new ItemStack(Items.FIREWORK_STAR, 1);

        result.set(DataComponents.FIREWORK_EXPLOSION, new FireworkExplosion(
                shape,
                colors,
                IntList.of(),
                hasTrail,
                hasTwinkle
        ));

        ResolvableProfile profile = headStack.get(DataComponents.PROFILE);
        if (profile != null) {
            result.set(DataComponents.PROFILE, profile);
            result.set(DataComponents.CUSTOM_NAME, Component.literal(buildFireworkName(profile)));
        }

        return result;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        // プレイヤーヘッドはクラフト後も消費せずグリッドに残す
        // (バニラの処理は「1個decrement→remainingを加算」の順で動くため、
        //  ここで返すのは必ず個数1にする。元の個数をそのまま返すと二重加算されて増えてしまう)
        NonNullList<ItemStack> remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.is(Items.PLAYER_HEAD) && stack.has(DataComponents.PROFILE)) {
                remaining.set(i, stack.copyWithCount(1));
            }
        }
        return remaining;
    }

    private static String profileName(ResolvableProfile profile) {
        return profile == null ? null : profile.name().orElse(null);
    }

    static String buildFireworkName(ResolvableProfile profile) {
        String playerName = profile.name().orElse(null);
        if (playerName == null || playerName.isEmpty()) {
            return "プレイヤー(名称不明)花火";
        }
        return playerName + "花火";
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}
