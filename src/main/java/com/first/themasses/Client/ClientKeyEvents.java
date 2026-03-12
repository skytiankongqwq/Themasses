package com.first.themasses.Client;

import com.first.themasses.TheMasses;
import com.first.themasses.curios.mysterious.*;
import com.first.themasses.curios.ring.heishatangring;
import com.first.themasses.networking.ModMessages;
import com.first.themasses.networking.PacketAbilityKey;
import com.first.themasses.Recipes.KeyInit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
//按键效果检测
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, value = Dist.CLIENT)
public class ClientKeyEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 监听 ABILITY_KEY (R键)
        while (KeyInit.ABILITY_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // --- 情况 1: Shift + R (切换戒指模式) ---
            if (Screen.hasShiftDown()) {
                // 检查是否佩戴了戒指
                boolean hasRing = CuriosApi.getCuriosHelper()
                        .findFirstCurio(player, stack -> stack.getItem() instanceof heishatangring)
                        .isPresent();
                if (hasRing) {
                    ModMessages.sendToServer(new PacketAbilityKey(-2));
                }
            }
            // --- 情况 2: 单纯按 R (站立时触发另一个技能) ---
            // 1. XiaoArin (分配标识符 -888  )
            boolean hasArin = CuriosApi.getCuriosHelper()
                    .findFirstCurio(player, stack -> stack.getItem() instanceof XiaoArin)
                    .isPresent();
            if (hasArin) {
                ModMessages.sendToServer(new PacketAbilityKey(-888));
            }

            // 2. Player7(分配标识符 -777)
            else if (CuriosApi.getCuriosHelper()
                    .findFirstCurio(player, stack -> stack.getItem() instanceof Playerseven)
                    .isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-777));
            }
            //3.昆仲饰品(分配标识符 -999)
            else if (CuriosApi.getCuriosHelper()
                    .findFirstCurio(player, stack -> stack.getItem() instanceof KunZhong)
                    .isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-999));
            }
            // 4. Baizeli (分配标识符 -555)
            else if (CuriosApi.getCuriosHelper()
                    .findFirstCurio(player, stack -> stack.getItem() instanceof Baizeli)
                    .isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-666));
            }
            // 5. Duvacein (分配标识符 -555) ---
            else if (CuriosApi.getCuriosHelper()
                    .findFirstCurio(player, stack -> stack.getItem() instanceof Duvacein)
                    .isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-555));
            }
            // 6. 检测 XIN_TIRIS 饰品 (标识符为 -444)
            else if (CuriosApi.getCuriosHelper()
                    .findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS)
                    .isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-444));
            }
            // 7.准星目标为Dazhi偏转判定逻辑
            else {
                Entity target = Minecraft.getInstance().crosshairPickEntity;
                int targetId = (target != null) ? target.getId() : -1;
                ModMessages.sendToServer(new PacketAbilityKey(targetId));
            }

        }
    }
}