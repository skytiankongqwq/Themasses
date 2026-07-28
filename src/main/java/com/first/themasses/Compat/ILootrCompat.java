package com.first.themasses.Compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ILootrCompat {
    // 返回是否成功执行了 Lootr 专项重构
    boolean doLootrRefactor(ServerLevel level, Entity target, ContainerEntity targetContainer,
                            ServerPlayer player, List<ItemStack> items, ResourceLocation tableLoc);
}