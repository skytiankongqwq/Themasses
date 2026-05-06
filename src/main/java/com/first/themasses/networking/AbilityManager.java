package com.first.themasses.networking;

import com.first.themasses.curios.all.MarbleShardItem;
import com.first.themasses.curios.mysterious.*;
import com.first.themasses.curios.ring.heishatangring;
import com.first.themasses.events.mysterious.DuvaceinEvents;
import com.first.themasses.events.mysterious.wu1wu2EvolutionHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 统一管理服务端能力触发逻辑
 */
@SuppressWarnings("removal")
public class AbilityManager {
    // 存储 ID 与 执行逻辑的映射
    private static final Map<Integer, Consumer<ServerPlayer>> REGISTRY = new HashMap<>();

    static {
        // --- 注册所有负数 ID 对应的能力逻辑 ---

        // XiaoArin (-888)
        registerCurioAbility(-888, XiaoArin.class, (player, stack) -> XiaoArin.triggerTimeStop(player));
        // Playerseven (-777)
        registerCurioAbility(-777, Playerseven.class, (player, stack) -> Playerseven.triggerAbility(player));
        // Baizeli (-666)
        registerCurioAbility(-666, Baizeli.class, (player, stack) -> ((Baizeli) stack.getItem()).startSkill(stack, player));
        // 戒指切换 (-2)
        registerCurioAbility(-2, heishatangring.class, (player, stack) -> heishatangring.toggleMode(player));
        // KunZhong (-999)
        registerCurioAbility(-999, KunZhong.class, (player, stack) -> {
            KunZhong item = (KunZhong) stack.getItem();
            if (stack.getOrCreateTag().getBoolean("SkillActive")) {
                item.stopSkill(stack, player);
            } else {
                item.startSkill(stack, player);
            }
        });
        // Duvacein (-555) - 特殊逻辑（不依赖 Curios 容器实例）
        REGISTRY.put(-555, player -> {
            DuvaceinEvents.isEnabled = !DuvaceinEvents.isEnabled;
            String status = DuvaceinEvents.isEnabled ? "§a开启" : "§c关闭";
            player.displayClientMessage(Component.literal("幽匿感染已 " + status), true);
        });
        // XIN_TIRIS (-444)
        registerCurioAbility(-444, XIN_TIRIS.class, (player, stack) -> XIN_TIRIS.executeXINTirisSkill(player));
        // MarbleShard (-5152)
        registerCurioAbility(-5152, MarbleShardItem.class, (player, stack) -> MarbleShardItem.executeMarbleShardSkill(player));
        // haozi (-333)
        registerCurioAbility(-333, haozi.class, (player, stack) -> haozi.executeHaoziSkill(player));
        // wu1wu2 (-51)
        registerCurioAbility(-51, wu1wu2.class, (player, stack) -> wu1wu2EvolutionHandler.handleAbilityPacket(player, -51));
    }

    /**
     * 辅助方法：快速注册需要检查 Curios 物品的能力
     */
    private static <T> void registerCurioAbility(int id, Class<T> clazz, AbilityExecutor executor) {
        REGISTRY.put(id, player -> {
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> clazz.isInstance(stack.getItem()))
                    .ifPresent(slot -> executor.execute(player, slot.stack()));
        });
    }

    public static void execute(int id, ServerPlayer player) {
        if (REGISTRY.containsKey(id)) {
            REGISTRY.get(id).accept(player);
        }
    }

    @FunctionalInterface
    interface AbilityExecutor {
        void execute(ServerPlayer player, ItemStack stack);
    }
}