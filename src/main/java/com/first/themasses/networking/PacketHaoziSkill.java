package com.first.themasses.networking;

import com.first.themasses.curios.mysterious.haozi;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.function.Supplier;

public class PacketHaoziSkill {
    public PacketHaoziSkill() {}

    public PacketHaoziSkill(FriendlyByteBuf buf) {}

    public void toBytes(FriendlyByteBuf buf) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // 验证玩家确实戴着这个饰品（防止作弊包）
                CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof haozi).ifPresent(slot -> {
                    // 核心调用：执行你写的那个静态方法
                    haozi.executeHaoziSkill(player);
                });
            }
        });
        return true;
    }
}