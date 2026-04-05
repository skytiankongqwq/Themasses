package com.first.themasses.Client;

import com.first.themasses.TheMasses;
import com.first.themasses.curios.all.MarbleShardItem;
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

        while (KeyInit.ABILITY_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            // 标记位：用于判断最后是否需要执行“准星目标”逻辑
            boolean anyAbilityTriggered = false;

            // --- 情况 1: Shift + R (切换戒指模式) ---
            if (Screen.hasShiftDown()) {
                if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatangring).isPresent()) {
                    ModMessages.sendToServer(new PacketAbilityKey(-2));
                    anyAbilityTriggered = true;
                }
            }

            // --- 情况 2: 常规触发 (全部改为独立 if，实现同步发包) ---

            // 1. XiaoArin (-888)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XiaoArin).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-888));
                anyAbilityTriggered = true;
            }

            // 2. Player7 (-777)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof Playerseven).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-777));
                anyAbilityTriggered = true;
            }

            // 3. 昆仲饰品 (-999)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof KunZhong).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-999));
                anyAbilityTriggered = true;
            }

            // 4. Baizeli (-666)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof Baizeli).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-666));
                anyAbilityTriggered = true;
            }

            // 5. Duvacein (-555)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof Duvacein).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-555));
                anyAbilityTriggered = true;
            }

            // 6. XIN_TIRIS (-444)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-444));
                anyAbilityTriggered = true;
            }

            // 7. 大理石碎片 (-5152)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof MarbleShardItem).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-5152));
                anyAbilityTriggered = true;
            }

            // 8. haozi (-333)
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof haozi).isPresent()) {
                ModMessages.sendToServer(new PacketAbilityKey(-333));
                anyAbilityTriggered = true;
            }

            // --- 情况 3: 默认兜底逻辑 ---
            // 只有当以上任何一件饰品都没穿时，才触发准星目标判定
            if (!anyAbilityTriggered) {
                Entity target = Minecraft.getInstance().crosshairPickEntity;
                int targetId = (target != null) ? target.getId() : -1;
                ModMessages.sendToServer(new PacketAbilityKey(targetId));
            }
        }
    }
}