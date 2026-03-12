package com.first.themasses.networking;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
//视角翻转数据包
public class ViewFlipPacket {

    // 构造函数：数据包不携带数据，只需要指令
    public ViewFlipPacket() {}

    // 编码：不需要写入数据
    public static void encode(ViewFlipPacket msg, FriendlyByteBuf buf) {
        // 无操作
    }

    // 解码：不需要读取数据
    public static ViewFlipPacket decode(FriendlyByteBuf buf) {
        return new ViewFlipPacket();
    }

    // 处理：在客户端执行视角翻转
    public static void handle(ViewFlipPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            // 确保我们在客户端线程上运行
            if (Minecraft.getInstance().player != null) {

                // 获取当前玩家
                Minecraft mc = Minecraft.getInstance();
                float currentYRot = mc.player.getYRot();

                // 客户端直接修改自己的视角
                float newYRot = currentYRot + 180.0F;

                // 强制设置视角
                mc.player.setYRot(newYRot);
                mc.player.setYHeadRot(newYRot);

                // 可选：重置 client yaw/pitch 变量（确保后续鼠标输入基于新角度）
                // mc.player.yRotO = newYRot; 
            }
        });
        context.setPacketHandled(true);
    }
}