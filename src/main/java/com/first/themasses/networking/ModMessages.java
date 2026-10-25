package com.first.themasses.networking;

import com.first.themasses.TheMasses;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
@SuppressWarnings("removal")
//数据包注册
public class ModMessages {
    private static final String PROTOCOL_VERSION = "1";
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    // 唯一的通信通道实例
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TheMasses.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 【重要】在 Mod 主类 (TheMasses) 的构造函数中调用此方法
     */
    public static void register() {
        // 1.(按键触发能力)
        INSTANCE.messageBuilder(PacketAbilityKey.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketAbilityKey::decode)
                .encoder(PacketAbilityKey::encode)
                .consumerMainThread(PacketAbilityKey::handle)
                .add();

        // 2.(屏幕翻转/同步包)
        INSTANCE.messageBuilder(ViewFlipPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(ViewFlipPacket::decode)
                .encoder(ViewFlipPacket::encode)
                .consumerMainThread(ViewFlipPacket::handle)
                .add();

        // 注册变身包
        INSTANCE.messageBuilder(PacketSyncHuanpi.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncHuanpi::decode)
                .encoder(PacketSyncHuanpi::encode)
                .consumerMainThread(PacketSyncHuanpi::handle)
                .add();
        // m3技能
        INSTANCE.messageBuilder(PacketXINTirisSkill.class, id())
                .decoder(PacketXINTirisSkill::new)
                .encoder(PacketXINTirisSkill::toBytes)
                .consumerMainThread(PacketXINTirisSkill::handle)
                .add();

        // Miku 特效状态同步
        INSTANCE.messageBuilder(PacketMikuEffect.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketMikuEffect::decode)
                .encoder(PacketMikuEffect::encode)
                .consumerMainThread(PacketMikuEffect::handle)
                .add();

        INSTANCE.messageBuilder(PacketSyncSimTime.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncSimTime::new) // 使用构造函数解码
                .encoder(PacketSyncSimTime::encode)
                .consumerMainThread(PacketSyncSimTime::handle)
                .add();

        // 如果以后有新的数据包，直接在这里复制上面的模板，id() 会自动递增
    }

    /**
     * 发送给服务端 (主要用于按键监听)
     */
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    /**
     * 发送给特定玩家 (主要用于服务端同步数据给客户端)
     */
    /** 发送给正在追踪该实体的所有玩家（含自己）。 */
    public static <MSG> void sendToTracking(MSG message, net.minecraft.world.entity.Entity entity) {
        INSTANCE.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity), message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}