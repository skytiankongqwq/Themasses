package com.first.themasses.events.buff;

import com.first.themasses.TheMasses;
import com.first.themasses.effect.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
//坑人药水的放置方块逻辑
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerAntiGravityEvent {

    private static final double ACTIVATION_CHANCE = 0.5; // 20% 概率触发
    private static final double UPWARD_VELOCITY = 8;    // 初始向上速度
    private static final double DOWNWARD_VELOCITY = -30; // 向下砸的速度 (负值表示向下)
    private static final double TARGET_HEIGHT = 20.0;     // 目标高度：20格

    // 存储玩家触发时间和初始位置的Map
    private static final Map<UUID, PlayerLaunchData> launchedPlayers = new HashMap<>();

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {

        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {

            if (player.hasEffect(ModEffects.BAMBOOZLED.get())) {

                if (player.level().random.nextDouble() < ACTIVATION_CHANCE) {

                    launchPlayer(player);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        // 只在服务端处理，并且是每个tick的开始阶段
        if (event.phase == TickEvent.Phase.START && !event.player.level().isClientSide) {
            Player player = event.player;
            UUID playerId = player.getUUID();

            // 检查玩家是否在飞行记录中
            if (launchedPlayers.containsKey(playerId)) {
                PlayerLaunchData data = launchedPlayers.get(playerId);

                // 计算当前高度差 (相对于触发时的Y坐标)
                double currentHeight = player.getY() - data.initialY;

                // 如果高度差达到或超过目标高度，施加向下的力
                if (currentHeight >= TARGET_HEIGHT && !data.hasDropped) {
                    applyDownwardForce(player);
                    data.hasDropped = true;
                    data.dropTime = player.level().getGameTime();
                }

                // 检查玩家是否落地或飞行时间过长（防止卡在半空）
                boolean isOnGround = player.onGround() || player.isInWater() || player.isInLava();
                long currentTime = player.level().getGameTime();

                // 如果玩家已经落地，或者向下掉落开始后超过5秒，移除记录
                if ((isOnGround && currentTime - data.dropTime > 10) ||
                        (data.hasDropped && currentTime - data.dropTime > 100)) { // 5秒超时
                    launchedPlayers.remove(playerId);
                }
            }
        }
    }

    private static void launchPlayer(Player player) {
        // 记录玩家初始数据
        PlayerLaunchData data = new PlayerLaunchData(
                player.getY(),
                player.level().getGameTime()
        );
        launchedPlayers.put(player.getUUID(), data);

        // 设置一个向上的速度矢量
        // 注意：Y方向使用UPWARD_VELOCITY，X和Z方向保持原有水平速度
        Vec3 bounce = new Vec3(
                player.getDeltaMovement().x * 0.5,  // 减小水平速度，让飞行更垂直
                UPWARD_VELOCITY,
                player.getDeltaMovement().z * 0.5
        );

        // 应用速度
        player.setDeltaMovement(bounce);

        // 关键：对于 ServerPlayer，必须设置此标志才能同步到客户端
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtMarked = true;
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));
        }

        // 可选：播放音效或粒子效果
        // player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
        //     SoundEvents.FIREWORK_ROCKET_LAUNCH, player.getSoundSource(), 1.0F, 1.0F);
    }

    private static void applyDownwardForce(Player player) {
        // 瞬间施加一个强大的向下的力
        // 将水平速度清零，只保留垂直向下的速度
        Vec3 slam = new Vec3(0, DOWNWARD_VELOCITY, 0);

        // 应用速度
        player.setDeltaMovement(slam);

        // 关键：对于 ServerPlayer，必须设置此标志才能同步到客户端
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.hurtMarked = true;
            serverPlayer.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(serverPlayer));

            // 可选：给玩家发送一个消息
            // serverPlayer.sendSystemMessage(Component.literal("重力恢复正常...有点太正常了！"));
        }

        // 可选：播放下落音效
        // player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
        //     SoundEvents.GENERIC_EXPLODE, player.getSoundSource(), 0.5F, 1.5F);
    }

    // 存储玩家触发时数据的内部类
    private static class PlayerLaunchData {
        public final double initialY;
        public final long launchTime;
        public boolean hasDropped;
        public long dropTime;

        public PlayerLaunchData(double initialY, long launchTime) {
            this.initialY = initialY;
            this.launchTime = launchTime;
            this.hasDropped = false;
            this.dropTime = 0;
        }
    }
}