package com.first.themasses.events.prop;

import com.first.themasses.item.prop.SummonerScythe;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "themasses")
public class SummonerEvents {

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        // 确保击杀者是玩家
        if (event.getSource().getEntity() instanceof Player player) {
            // 遍历玩家背包寻找该物品（或者只检测主副手）
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);

                if (stack.getItem() instanceof SummonerScythe) {
                    CompoundTag nbt = stack.getOrCreateTag();
                    int charges = nbt.getInt("Charges");

                    // 如果次数已满 1124，不再计数
                    if (charges >= 1124) continue;

                    int kills = nbt.getInt("Kills") + 1;

                    if (kills >= 10) {
                        nbt.putInt("Kills", 0);
                        // 增加一次召唤机会，但不超过上限
                        nbt.putInt("Charges", Math.min(charges + 1, 1124));
                    } else {
                        nbt.putInt("Kills", kills);
                    }

                    // 找到一个物品处理完即可，防止背一包勋章刷经验
                    break;
                }
            }
        }
    }
}