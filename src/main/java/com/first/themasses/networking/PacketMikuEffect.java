package com.first.themasses.networking;

import com.first.themasses.Client.miku.MikuManager;
import com.first.themasses.Client.miku.MikuPawManager;
import com.first.themasses.Client.miku.MikuSupremeManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 把服务端的 Miku 特效状态同步给追踪该实体的客户端。
 * <p>Forge 的 PersistentData 不会自动同步，所以需要这个包。</p>
 */
public class PacketMikuEffect {

    public static final byte TYPE_MIKU = 0;
    public static final byte TYPE_PAW = 1;
    public static final byte TYPE_SUPREME = 2;

    private final int entityId;
    private final byte type;
    private final boolean active;
    private final int durationSeconds;

    public PacketMikuEffect(int entityId, byte type, boolean active, int durationSeconds) {
        this.entityId = entityId;
        this.type = type;
        this.active = active;
        this.durationSeconds = durationSeconds;
    }

    public static void encode(PacketMikuEffect msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.entityId);
        buf.writeByte(msg.type);
        buf.writeBoolean(msg.active);
        buf.writeVarInt(msg.durationSeconds);
    }

    public static PacketMikuEffect decode(FriendlyByteBuf buf) {
        return new PacketMikuEffect(buf.readVarInt(), buf.readByte(),
                buf.readBoolean(), buf.readVarInt());
    }

    public static void handle(PacketMikuEffect msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientHandler.apply(msg)));
        context.setPacketHandled(true);
    }

    /**
     * 只在客户端加载，避免服务端类加载器碰到 Minecraft 客户端类。
     */
    private static final class ClientHandler {

        private ClientHandler() {
        }

        static void apply(PacketMikuEffect msg) {
            var level = net.minecraft.client.Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            Entity entity = level.getEntity(msg.entityId);
            if (entity == null) {
                return;
            }
            switch (msg.type) {
                case TYPE_MIKU -> {
                    if (msg.active) {
                        MikuManager.activate(entity, msg.durationSeconds);
                    } else {
                        MikuManager.deactivate(entity);
                    }
                }
                case TYPE_PAW -> {
                    if (msg.active) {
                        MikuPawManager.activate(entity, msg.durationSeconds);
                    } else {
                        MikuPawManager.deactivate(entity);
                    }
                }
                case TYPE_SUPREME -> {
                    if (msg.active) {
                        MikuSupremeManager.activate(entity, msg.durationSeconds);
                    } else {
                        MikuSupremeManager.deactivate(entity);
                    }
                }
                default -> {
                }
            }
        }
    }
}
