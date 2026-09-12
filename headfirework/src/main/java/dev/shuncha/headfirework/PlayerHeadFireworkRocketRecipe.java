package dev.shuncha.headfirework;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PlayerHeadFireworkRocketRecipe extends CustomRecipe {

    /** 複数プレイヤー分のオーナー情報を、ロケットのCUSTOM_DATAコンポーネントに保存する際のキー。 */
    public static final String OWNERS_TAG_KEY = "HeadFireworkOwners";

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
                // 異なるプレイヤーの星が混ざっていても許可する(1つのロケットで複数人の顔を表示できるようにするため)
                star += stack.getCount();
            } else {
                other++;
            }
        }

        // 紙は星の個数分だけ必要(星1個につき紙1枚)
        return gunpowder >= 1 && gunpowder <= 3
                && star >= 1
                && paper == star
                && other == 0;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        List<FireworkExplosion> explosions = new ArrayList<>();
        List<ResolvableProfile> owners = new ArrayList<>();
        int gunpowderCount = 0;

        for (ItemStack stack : input.items()) {
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.is(Items.FIREWORK_STAR)) {
                FireworkExplosion explosion = stack.get(DataComponents.FIREWORK_EXPLOSION);
                ResolvableProfile profile = stack.get(DataComponents.PROFILE);
                for (int i = 0; i < stack.getCount(); i++) {
                    if (explosion != null) {
                        explosions.add(explosion);
                    }
                    owners.add(profile);
                }
            } else if (stack.is(Items.GUNPOWDER)) {
                gunpowderCount += stack.getCount();
            }
        }

        int starCount = owners.size();
        ItemStack result = new ItemStack(Items.FIREWORK_ROCKET, 3 * Math.max(starCount, 1));
        result.set(DataComponents.FIREWORKS, new Fireworks(gunpowderCount, explosions));

        if (!owners.isEmpty()) {
            // 先頭のオーナーをPROFILEコンポーネントに設定(ツールチップ表示・後方互換用)
            ResolvableProfile firstProfile = owners.get(0);
            if (firstProfile != null) {
                result.set(DataComponents.PROFILE, firstProfile);
            }

            // 全員分のオーナー情報を、explosionsと同じ並び順でCUSTOM_DATAに保存する
            // (爆発時に「何番目の爆発が誰の顔か」を引くための対応表として使う)
            ListTag ownersTag = new ListTag();
            for (ResolvableProfile profile : owners) {
                if (profile == null) {
                    continue;
                }
                ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, profile)
                        .resultOrPartial(err -> HeadFireworkMod.LOGGER.warn("HeadFirework: failed to encode owner profile: {}", err))
                        .ifPresent(ownersTag::add);
            }
            CompoundTag customDataTag = new CompoundTag();
            customDataTag.put(OWNERS_TAG_KEY, ownersTag);
            result.set(DataComponents.CUSTOM_DATA, CustomData.of(customDataTag));

            Set<String> uniqueNames = new LinkedHashSet<>();
            for (ResolvableProfile profile : owners) {
                if (profile != null) {
                    uniqueNames.add(PlayerHeadFireworkStarRecipe.buildFireworkName(profile)
                            .replace("花火", ""));
                }
            }
            result.set(DataComponents.CUSTOM_NAME, Component.literal(String.join("・", uniqueNames) + "花火"));
        }

        return result;
    }

    /**
     * ロケットのCUSTOM_DATAコンポーネントから、爆発順に対応した複数オーナーのリストを取り出す。
     * 保存されていない(単一オーナーの古い形式、または想定外のロケット)場合はnullを返す。
     */
    public static List<ResolvableProfile> readOwners(ItemStack fireworkStack) {
        CustomData customData = fireworkStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            return null;
        }
        CompoundTag tag = customData.copyTag();
        if (!tag.contains(OWNERS_TAG_KEY)) {
            return null;
        }
        Tag rawList = tag.get(OWNERS_TAG_KEY);
        if (!(rawList instanceof ListTag listTag)) {
            return null;
        }

        List<ResolvableProfile> owners = new ArrayList<>();
        for (Tag element : listTag) {
            ResolvableProfile.CODEC.parse(NbtOps.INSTANCE, element)
                    .resultOrPartial(err -> HeadFireworkMod.LOGGER.warn("HeadFirework: failed to decode owner profile: {}", err))
                    .ifPresent(owners::add);
        }
        return owners;
    }

    @Override
    public RecipeSerializer<? extends CustomRecipe> getSerializer() {
        return SERIALIZER;
    }
}