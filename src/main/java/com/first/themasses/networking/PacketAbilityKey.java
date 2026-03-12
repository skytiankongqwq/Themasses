package com.first.themasses.networking;

import com.first.themasses.curios.mysterious.Baizeli;
import com.first.themasses.curios.mysterious.Playerseven;
import com.first.themasses.curios.mysterious.XIN_TIRIS;
import com.first.themasses.curios.mysterious.XiaoArin;
import com.first.themasses.curios.ring.heishatangring;
import com.first.themasses.events.mysterious.DuvaceinEvents;
import com.first.themasses.events.mysterious.HeishatangEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

//按键事件检测
public class PacketAbilityKey {
    private final int targetId;

    public PacketAbilityKey(int targetId) {
        this.targetId = targetId;
    }

    public static void encode(PacketAbilityKey msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.targetId);
    }

    public static PacketAbilityKey decode(FriendlyByteBuf buf) {
        return new PacketAbilityKey(buf.readInt());
    }

    public static void handle(PacketAbilityKey msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // --- 1. 处理 XiaoArin 饰品技能信号 (-888) ---
            if (msg.targetId == -888) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XiaoArin)
                        .ifPresent(slot -> {
                            // 调用 XiaoArin 类中的静态触发方法
                            XiaoArin.triggerTimeStop(player);
                        });
            }

            // --- 2. 收到 Playerseven 饰品技能信号 (-777) ---
            else if (msg.targetId == -777) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof Playerseven)
                        .ifPresent(slot -> {
                            Playerseven.triggerAbility(player);
                        });
            }

            // --- 3. 收到白泽 (Baizeli) 饰品技能信号 (-666) ---
            else if (msg.targetId == -666) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof Baizeli)
                        .ifPresent(slot -> {
                            ItemStack stack = slot.stack();
                            // 调用 Baizeli 类中定义的技能启动方法
                            ((Baizeli) stack.getItem()).startSkill(stack, player);
                        });
            }

            // --- 4. 收到戒指切换信号 (-2) ---
            else if (msg.targetId == -2) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatangring)
                        .ifPresent(slot -> {
                            heishatangring.toggleMode(player);
                        });
            }

            // --- 5. 处理指向性技能 (targetId >= 0) ---
            else if (msg.targetId >= 0) {
                Entity target = player.level().getEntity(msg.targetId);
                if (target instanceof LivingEntity livingTarget) {
                    HeishatangEvents.executeHeishatangAbility(player, livingTarget);
                }
            }

            // --- 6. targetId == -1 的空按逻辑 ---
            else if (msg.targetId == -1) {
                // 预留位置
            }

            // --- 7. 处理 KunZhong (坤中) 饰品技能信号 (-999) ---
            else if (msg.targetId == -999) {
                // 查找玩家身上是否有“坤中”饰品
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof com.first.themasses.curios.mysterious.KunZhong)
                        .ifPresent(slot -> {
                            // 获取饰品堆栈
                            net.minecraft.world.item.ItemStack stack = slot.stack();
                            com.first.themasses.curios.mysterious.KunZhong item = (com.first.themasses.curios.mysterious.KunZhong) stack.getItem();

                            // 逻辑判定：如果技能已开启，则关闭；如果未开启，则开启
                            if (stack.getOrCreateTag().getBoolean("SkillActive")) {
                                item.stopSkill(stack, player);
                            } else {
                                item.startSkill(stack, player);
                            }
                        });
            }

            // --- 8. 收到 Playerseven 饰品技能信号 (-777) ---
            else if (msg.targetId == -555) {
                // 切换开关状态
                DuvaceinEvents.isEnabled = !DuvaceinEvents.isEnabled;

                // 给玩家发个消息提示，省得不知道开没开 (可选)
                String status = DuvaceinEvents.isEnabled ? "§a开启" : "§c关闭";
                player.displayClientMessage(Component.literal("幽匿感染已 " + status), true);
            }
            // --- 9. 收到 XIN_TIRIS 饰品技能信号 (-444) ---
            else if (msg.targetId == -444) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS)
                        .ifPresent(slot -> {
                            XIN_TIRIS.executeXINTirisSkill(player);
                        });
            }



        });
        ctx.get().setPacketHandled(true);
    }
}