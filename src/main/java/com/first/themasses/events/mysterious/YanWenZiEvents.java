package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.YanWenZi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

@Mod.EventBusSubscriber(modid = "themasses") // 确保这里的 modid 和你项目的一致
public class YanWenZiEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDeathDefy(LivingDeathEvent event) {
        // 1. 只在服务端处理玩家死亡
        if (event.getEntity() instanceof ServerPlayer player) {

            // 2. 查找玩家是否佩戴了“颜文字”
            CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof YanWenZi).ifPresent(slot -> {
                ItemStack stack = slot.stack();

                // 3. 检查冷却逻辑 (40秒冷却，由 getCooldowns 管理)
                if (!player.getCooldowns().isOnCooldown(stack.getItem())) {

                    // 核心步骤：取消死亡事件！
                    event.setCanceled(true);

                    // 4. 调用物品类里的触发逻辑（回复、击飞、BUFF）
                    YanWenZi.triggerDeathDefy(player, stack);
                }
            });
        }
    }
}