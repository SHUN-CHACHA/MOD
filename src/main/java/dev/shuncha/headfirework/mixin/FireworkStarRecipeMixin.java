package dev.shuncha.headfirework.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.FireworkStarRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireworkStarRecipe.class)
public class FireworkStarRecipeMixin {

    @Inject(
            method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void headfirework$excludePlayerHead(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        for (ItemStack stack : input.items()) {
            if (stack.is(Items.PLAYER_HEAD) && stack.has(DataComponents.PROFILE)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}