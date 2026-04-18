package com.first.themasses.networking;

import com.first.themasses.curios.all.MarbleShardItem;
import com.first.themasses.curios.mysterious.*;
import com.first.themasses.curios.ring.heishatangring;
import com.first.themasses.events.mysterious.DuvaceinEvents;
import com.first.themasses.events.mysterious.HeishatangEvents;
import com.first.themasses.events.mysterious.wu1wu2EvolutionHandler;
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

            int id = msg.targetId;

            // --- 1. XiaoArin (-888) ---
            if (id == -888) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XiaoArin)
                        .ifPresent(slot -> XiaoArin.triggerTimeStop(player));
            }

            // --- 2. Playerseven (-777) ---
            if (id == -777) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof Playerseven)
                        .ifPresent(slot -> Playerseven.triggerAbility(player));
            }

            // --- 3. Baizeli (-666) ---
            if (id == -666) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof Baizeli)
                        .ifPresent(slot -> {
                            ItemStack stack = slot.stack();
                            ((Baizeli) stack.getItem()).startSkill(stack, player);
                        });
            }

            // --- 4. 戒指切换 (-2) ---
            if (id == -2) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatangring)
                        .ifPresent(slot -> heishatangring.toggleMode(player));
            }

            // --- 5. KunZhong (-999) ---
            if (id == -999) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof KunZhong)
                        .ifPresent(slot -> {
                            ItemStack stack = slot.stack();
                            KunZhong item = (KunZhong) stack.getItem();
                            if (stack.getOrCreateTag().getBoolean("SkillActive")) {
                                item.stopSkill(stack, player);
                            } else {
                                item.startSkill(stack, player);
                            }
                        });
            }

            // --- 6. Duvacein (-555) ---
            if (id == -555) {
                DuvaceinEvents.isEnabled = !DuvaceinEvents.isEnabled;
                String status = DuvaceinEvents.isEnabled ? "§a开启" : "§c关闭";
                player.displayClientMessage(Component.literal("幽匿感染已 " + status), true);
            }

            // --- 7. XIN_TIRIS (-444) ---
            if (id == -444) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS)
                        .ifPresent(slot -> XIN_TIRIS.executeXINTirisSkill(player));
            }

            // --- 8. MarbleShard (-5152) ---
            if (id == -5152) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof MarbleShardItem)
                        .ifPresent(slot -> MarbleShardItem.executeMarbleShardSkill(player));
            }

            // --- 9. haozi (-333) ---
            if (id == -333) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof haozi)
                        .ifPresent(slot -> haozi.executeHaoziSkill(player));
            }

            // --- 10. wu1wu2 (-51) ---
            if (id == -51) {
                // 直接使用 player，IDE 已经知道它是 ServerPlayer
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof wu1wu2)
                        .ifPresent(slot -> {wu1wu2EvolutionHandler.handleAbilityPacket(player, -51);});
            }

            // --- 11. 指向性/实体判定 (仅当 id >= 0 时触发) ---
            if (id >= 0) {
                Entity target = player.level().getEntity(id);
                if (target instanceof LivingEntity livingTarget) {
                    HeishatangEvents.executeHeishatangAbility(player, livingTarget);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}