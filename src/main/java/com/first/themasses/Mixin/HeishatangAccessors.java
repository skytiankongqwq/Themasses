package com.first.themasses.Mixin;

import com.first.themasses.curios.mysterious.heishatang;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
//黑砂糖事件
public class HeishatangAccessors {

    @Mixin(net.minecraft.world.entity.LivingEntity.class)
    public interface LivingEntityAccessor {
        @org.spongepowered.asm.mixin.gen.Accessor("jumping")
        boolean isJumping();
    }

    @Mixin(net.minecraft.world.entity.npc.Villager.class)
    public interface VillagerAccessor {
        @org.spongepowered.asm.mixin.gen.Invoker("updateSpecialPrices")
        void invokeUpdateSpecialPrices(Player player);
    }
}