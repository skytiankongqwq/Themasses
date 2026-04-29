package com.first.themasses.networking;

import com.first.themasses.events.mysterious.HeishatangEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端按键触发的数据包
 * targetId < 0 : 触发饰品/特殊能力
 * targetId >= 0: 触发针对特定实体的能力
 */
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

            // --- 分流处理 ---
            if (id < 0) {
                // 1. 处理饰品/系统逻辑 (使用注册表)
                AbilityManager.execute(id, player);
            } else {
                // 2. 处理实体指向逻辑 (仅当 ID 为正数且存在对应实体时)
                Entity target = player.level().getEntity(id);
                if (target instanceof LivingEntity livingTarget) {
                    HeishatangEvents.executeHeishatangAbility(player, livingTarget);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}