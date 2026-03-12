package com.first.themasses.networking;

import com.first.themasses.Client.mysterious.ClientHuanpiLogic;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncHuanpi {
    private final String name;
    private final String skinValue;
    private final String skinSig;

    public PacketSyncHuanpi(String name, String value, String sig) {
        // 使用空字符串代替 null，防止 FriendlyByteBuf 抛出异常
        this.name = name != null ? name : "";
        this.skinValue = value != null ? value : "";
        this.skinSig = sig != null ? sig : "";
    }

    public static void encode(PacketSyncHuanpi msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.name);
        buf.writeUtf(msg.skinValue);
        buf.writeUtf(msg.skinSig);
    }

    public static PacketSyncHuanpi decode(FriendlyByteBuf buf) {
        return new PacketSyncHuanpi(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketSyncHuanpi msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 在客户端线程执行皮肤修改/还原逻辑
            ClientHuanpiLogic.applyHuanpi(msg.name, msg.skinValue, msg.skinSig);
        });
        ctx.get().setPacketHandled(true);
    }

    // Getter 方法
    public String getName() { return name; }
    public String getValue() { return skinValue; }
    public String getSig() { return skinSig; }
}