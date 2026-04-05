package com.first.themasses.networking;

import com.first.themasses.curios.all.MarbleShardItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.function.Supplier;

public class MarbleShardSkillPacket {

    // 必须保留空构造函数
    public MarbleShardSkillPacket() {}

    // 必须有这个构造函数用于从网络读取（即使目前为空）
    public MarbleShardSkillPacket(FriendlyByteBuf buf) {}

    // 必须有这个方法用于写入网络（即使目前为空）
    public void toBytes(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ServerLevel level = player.serverLevel();

            // 1. 验证饰品佩戴情况
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack ->
                    stack.getItem() instanceof MarbleShardItem).ifPresent(slot -> {

                // 2. 技能逻辑
                AABB area = player.getBoundingBox().inflate(3.0D);
                List<Pig> pigs = level.getEntitiesOfClass(Pig.class, area);

                int affectedPigs = 0;
                for (Pig pig : pigs) {
                    // 仅对成年且不在发情期的猪生效
                    if (pig.getAge() == 0 && !pig.isInLove()) {
                        pig.setInLove(player);
                        affectedPigs++;

                        // 服务器生成粒子同步给所有周围玩家
                        level.sendParticles(ParticleTypes.HEART,
                                pig.getX(), pig.getY() + 0.5D, pig.getZ(),
                                5, 0.2, 0.2, 0.2, 0.0);
                    }
                }

                // 3. 反馈
                if (affectedPigs > 0) {
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.CHICKEN_EGG, SoundSource.PLAYERS, 1.0F, 0.8F);
                    player.displayClientMessage(Component.literal("§d大理石碎片让周围充满了爱意... (" + affectedPigs + " 只)"), true);
                } else {
                    player.displayClientMessage(Component.literal("§7附近没有可以接受共鸣的猪"), true);
                }
            });
        });
        context.setPacketHandled(true);
    }
}