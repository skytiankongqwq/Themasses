package com.first.themasses.Compat;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class LootrCompatImpl implements ILootrCompat {
    @Override
    public boolean doLootrRefactor(ServerLevel level, Entity target, ContainerEntity targetContainer,
                                   ServerPlayer player, List<ItemStack> items, ResourceLocation tableLoc) {
        try {
            // 获取 Lootr 虚拟库存
            noobanidus.mods.lootr.api.inventory.ILootrInventory lootrInv = noobanidus.mods.lootr.api.LootrAPI.getInventory(
                    level,
                    target.getUUID(),
                    target.blockPosition(),
                    player,
                    targetContainer::getContainerSize,
                    targetContainer::getDisplayName,
                    // 【终极修正】：根据 image_9b9941.png 报错，Player 必须在 Container 之前
                    (Player p, Container container, ResourceLocation table, long seed) -> {
                        container.clearContent();
                        for (int i = 0; i < Math.min(items.size(), container.getContainerSize()); i++) {
                            container.setItem(i, items.get(i).copy());
                        }
                    },
                    () -> tableLoc,
                    targetContainer::getLootTableSeed
            );

            if (lootrInv != null) {
                // 使用 NetworkHooks 确保 1.20.1 环境下能正确打开带附加数据的菜单
                NetworkHooks.openScreen(player, lootrInv, (FriendlyByteBuf buf) -> {
                    buf.writeUUID(target.getUUID());
                    buf.writeBlockPos(target.blockPosition());
                    buf.writeComponent(targetContainer.getDisplayName());
                });
                return true;
            }
        } catch (Exception e) {
            // 打印错误以便调试，如果这里报错，handledByLootr 会返回 false
            e.printStackTrace();
        }
        return false;
    }
}