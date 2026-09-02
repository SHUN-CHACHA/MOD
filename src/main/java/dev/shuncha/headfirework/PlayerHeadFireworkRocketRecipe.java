package dev.shuncha.headfirework;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class PlayerHeadFireworkRocketRecipe extends CustomRecipe {

    public static final MapCodec<PlayerHeadFireworkRocketRecipe> MAP_CODEC =
            MapCodec.unit(PlayerHeadFireworkRocketRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerHeadFireworkRocketRecipe> STREAM_CODEC =
            StreamCodec.unit(new PlayerHeadFireworkRocketRecipe());
    public static final RecipeSerializer<PlayerHeadFireworkRocketRecipe> SERIALIZER =
            new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int gunpowder = 0;
        int paper = 0;
        int star = 0;
        int other = 0;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Items.GUNPOWDER)) {
                gunpowder += stack.getCount();
            } else if (stack.is(Items.PAPER)) {
                paper += stack.getCount();
            } else if (stack.is(Items.FIREWORK_STAR) && stack.has(DataComponents.PROFILE)) {
                star += stack.getCount();
            } else {
                other++;
            }
        }

        return gunpowder >= 1 && gunpowder <= 3
                && paper == 1
                && star == 1
                && other == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        ItemStack starStack = ItemStack.EMPTY;
        int gunpowderCount = 0;

        for (ItemStack stack : input.items()) {
            if (stack.is(Items.FIREWORK_STAR)) {
                starStack = stack;
            } else if (stack.is(Items.GUNPOWDER)) {
                gunpowderCount += stack.getCount();
            }
        }

        ItemStack result = new ItemStack(Items.FIREWORK_ROCKET, 3);

        List<FireworkExplosion> explosions = new ArrayList<>();
        FireworkExplosion explosion = starStack.get(DataComponents.FIREWORK_EXPLOSION);
        if (explosion != null) {
            explosions.add(explosion);
        }
        result.set(DataComponents.FIREWORKS, new Fireworks(gunpowderCount, explosions));

        ResolvableProfile profile = starStack.get(DataComponents.PROFILE);
        if (profile != null) {
            result.set(DataComponents.PROFILE, profile);
            result.set(DataComponents.CUSTOM_NAME,
                    Component.literal(PlayerHeadFireworkStarRecipe.buildFireworkName(profile)));
        }

        return result;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}