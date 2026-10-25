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
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.LinkedHashMap;
import java.util.Map;
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, value = Dist.CLIENT)
public class ClientKeyEvents {

    // 使用 Map 存储：饰品类 -> 数据包ID
    // 使用 LinkedHashMap 保证遍历顺序（如果你对触发优先级有要求）
    private static final Map<Class<? extends Item>, Integer> ABILITY_REGISTRY = new LinkedHashMap<>();

    static {
        ABILITY_REGISTRY.put(Playerseven.class, -777);
        ABILITY_REGISTRY.put(KunZhong.class, -999);
        ABILITY_REGISTRY.put(Baizeli.class, -666);
        ABILITY_REGISTRY.put(Duvacein.class, -555);
        ABILITY_REGISTRY.put(XIN_TIRIS.class, -444);
        ABILITY_REGISTRY.put(MarbleShardItem.class, -5152);
        ABILITY_REGISTRY.put(haozi.class, -333);
        ABILITY_REGISTRY.put(wu1wu2.class, -51);
        ABILITY_REGISTRY.put(Sim.class, -1010);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        while (KeyInit.ABILITY_KEY.consumeClick()) {
            Player player = Minecraft.getInstance().player;
            if (player == null) return;

            boolean anyAbilityTriggered = false;

            // 1. 特殊逻辑：Shift + R (保持独立处理，因为涉及按键状态判断)
            if (Screen.hasShiftDown()) {
                if (isEquipped(player, heishatangring.class)) {
                    ModMessages.sendToServer(new PacketAbilityKey(-2));
                    anyAbilityTriggered = true;
                }
            }

            // 2. 自动化逻辑：遍历 Map 自动检查并发包
            for (Map.Entry<Class<? extends Item>, Integer> entry : ABILITY_REGISTRY.entrySet()) {
                if (isEquipped(player, entry.getKey())) {
                    ModMessages.sendToServer(new PacketAbilityKey(entry.getValue()));
                    anyAbilityTriggered = true;
                }
            }

            // 3. 兜底逻辑
            if (!anyAbilityTriggered) {
                Entity target = Minecraft.getInstance().crosshairPickEntity;
                int targetId = (target != null) ? target.getId() : -1;
                ModMessages.sendToServer(new PacketAbilityKey(targetId));
            }
        }
    }

    private static boolean isEquipped(Player player, Class<? extends Item> itemClass) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player,
                stack -> itemClass.isInstance(stack.getItem())).isPresent();
    }
}