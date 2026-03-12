package com.first.themasses.networking;

import com.first.themasses.curios.mysterious.XIN_TIRIS;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketXINTirisSkill {
    public PacketXINTirisSkill() {}

    public PacketXINTirisSkill(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // 这里回到服务端逻辑
            ServerPlayer player = context.getSender();
            if (player != null) {
                // 调用之前写好的静态处理逻辑
                // XIN_TIRIS.executeXINTirisSkill(player);
                // 注意：确保在该方法里检查了玩家是否佩戴了饰品，防止作弊器发包
            }
        });
        return true;
    }
}