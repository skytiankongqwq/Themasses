package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.Duvacein;
import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class DuvaceinEvents {
    private static final Random random = new Random();

    // --- 核心改动：开关变量 ---
    public static boolean isEnabled = false;

    private static final ConcurrentHashMap<BlockPos, BlockState> RECOVERY_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<BlockPos, Long> EXPIRY_CACHE = new ConcurrentHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        Level level = entity.level();

        if (level.isClientSide || !(entity instanceof Player player)) return;

        CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof Duvacein).ifPresent(slot -> {
            ServerLevel sl = (ServerLevel) level;
            long currentTime = sl.getGameTime();

            // 1. 生成逻辑：只有在 isEnabled 为 true 时才生成新块
            if (isEnabled && player.tickCount % 20 == 0) {
                BlockPos center = player.blockPosition().below();

                BlockPos.betweenClosedStream(center.offset(-4, 0, -4), center.offset(4, 0, 4)).forEach(targetPos -> {
                    BlockState current = sl.getBlockState(targetPos);

                    if (random.nextInt(100) < 20 && !current.isAir() && !current.is(Blocks.BEDROCK) && !current.is(Blocks.SCULK)) {
                        BlockPos frozenPos = targetPos.immutable();

                        if (!RECOVERY_CACHE.containsKey(frozenPos)) {
                            RECOVERY_CACHE.put(frozenPos, current);
                            EXPIRY_CACHE.put(frozenPos, currentTime + 60);

                            sl.setBlock(frozenPos, Blocks.SCULK.defaultBlockState(), 3);
                            sl.sendBlockUpdated(frozenPos, current, Blocks.SCULK.defaultBlockState(), 3);
                        }
                    }
                });
            }

            // 2. 自动还原逻辑：不受开关影响（确保关掉后地上的块能正常回收）
            EXPIRY_CACHE.forEach((pos, expiry) -> {
                if (currentTime >= expiry) {
                    restoreBlockSilently(sl, pos);
                }
            });

            // 3. 踩踏检测
            checkNearbyEntities(player, level);
        });
    }

    private static void restoreBlockSilently(ServerLevel sl, BlockPos pos) {
        BlockState original = RECOVERY_CACHE.remove(pos);
        EXPIRY_CACHE.remove(pos);
        if (original != null && sl.getBlockState(pos).is(Blocks.SCULK)) {
            sl.setBlock(pos, original, 3);
            sl.sendBlockUpdated(pos, Blocks.SCULK.defaultBlockState(), original, 3);
        }
    }

    private static void checkNearbyEntities(Player player, Level level) {
        level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0), e -> e != player).forEach(target -> {
            BlockPos pos = target.blockPosition();
            BlockPos below = pos.below();
            if (level.getBlockState(pos).is(Blocks.SCULK)) triggerWarden(level, target, pos);
            else if (level.getBlockState(below).is(Blocks.SCULK)) triggerWarden(level, target, below);
        });
    }

    private static void triggerWarden(Level level, LivingEntity target, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;

        BlockState originalState = RECOVERY_CACHE.remove(pos);
        EXPIRY_CACHE.remove(pos);
        if (originalState == null) originalState = Blocks.GRASS_BLOCK.defaultBlockState();

        sl.setBlock(pos, originalState, 3);
        sl.sendBlockUpdated(pos, Blocks.SCULK.defaultBlockState(), originalState, 3);

        net.minecraft.world.entity.monster.warden.Warden warden = new net.minecraft.world.entity.monster.warden.Warden(
                net.minecraft.world.entity.EntityType.WARDEN, level);
        warden.setPos(target.getX(), target.getY(), target.getZ());
        warden.setNoAi(true);
        level.addFreshEntity(warden);

        target.hurt(level.damageSources().magic(), 6.0F);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.WITHER, 100, 1));
        warden.setPose(net.minecraft.world.entity.Pose.ROARING);
        level.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.WARDEN_ROAR, net.minecraft.sounds.SoundSource.HOSTILE, 1.0F, 1.0F);

        sl.getServer().tell(new net.minecraft.server.TickTask(sl.getServer().getTickCount() + 20, () -> warden.discard()));
    }

    @SubscribeEvent
    public static void onRightClickGold(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        CuriosApi.getCuriosHelper().findFirstCurio(event.getEntity(), s -> s.getItem() instanceof Duvacein).ifPresent(slot -> {
            if (event.getLevel().getBlockState(event.getPos()).is(Blocks.GOLD_BLOCK)) {
                BlockPos.betweenClosedStream(event.getPos().offset(-1, -1, -1), event.getPos().offset(1, 1, 1)).forEach(p -> {
                    if (!event.getLevel().getBlockState(p).isAir()) {
                        event.getLevel().setBlock(p, Blocks.GOLD_BLOCK.defaultBlockState(), 3);
                    }
                });
            }
        });
    }
    // 专门处理头顶名字显示的事件
    @SubscribeEvent
    public static void onNameFormat(net.minecraftforge.event.entity.player.PlayerEvent.NameFormat event) {
        // 必须从 PersistentData 读取
        String huanpiName = event.getEntity().getPersistentData().getString("HuanpiName");
        if (huanpiName != null && !huanpiName.isEmpty()) {
            event.setDisplayname(net.minecraft.network.chat.Component.literal(huanpiName));
        }
    }

    // 可选：如果你想让变身后的名字在聊天框里也生效，可以加上这个
    @SubscribeEvent
    public static void onPlayerTabName(net.minecraftforge.event.entity.player.PlayerEvent.TabListNameFormat event) {
        if (event.getEntity().getPersistentData().contains("HuanpiName")) {
            String fakeName = event.getEntity().getPersistentData().getString("HuanpiName");
            event.setDisplayName(net.minecraft.network.chat.Component.literal(fakeName));
        }
    }
}