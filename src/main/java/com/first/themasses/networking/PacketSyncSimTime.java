package com.first.themasses.networking;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketSyncSimTime {
    private final long endTime;

    // 构造函数
    public PacketSyncSimTime(long endTime) {
        this.endTime = endTime;
    }

    // 解码 (Decoder)
    public PacketSyncSimTime(FriendlyByteBuf buf) {
        this.endTime = buf.readLong();
    }

    // 编码 (Encoder)
    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(this.endTime);
    }

    // 处理 (Consumer)
    public static void handle(PacketSyncSimTime pkt, Supplier<NetworkEvent.Context> ctxGetter) {
        NetworkEvent.Context ctx = ctxGetter.get();
        ctx.enqueueWork(() -> {
            // 只有在客户端执行
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                // 核心：把服务器传来的结束时间存入客户端 NBT
                player.getPersistentData().putLong("SimActiveEndTime", pkt.endTime);
            }
        });
        ctx.setPacketHandled(true);
    }
}