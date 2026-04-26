package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.MikuLa;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Guardian;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.PlayLevelSoundEvent; // 正确的包名
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;
@Mod.EventBusSubscriber(modid = "themasses")
public class MikuCatAmuletEvents {

    private static boolean hasCatAmulet(LivingEntity entity) {
        if (!(entity instanceof Player player)) return false;
        return CuriosApi.getCuriosHelper().findFirstCurio(player,
                stack -> stack.getItem() instanceof MikuLa).isPresent();
    }

    // 1. 静音掉原版的玩家受伤声
    @SubscribeEvent
    public static void onPlaySound(PlayLevelSoundEvent.AtEntity event) {
        Entity entity = event.getEntity();
        if (entity instanceof Player player && hasCatAmulet(player)) {
            // 检查是否是玩家受击音效
            String path = event.getSound().get().getLocation().getPath();
            if (path.contains("entity.player.hurt")) {
                // 将音量设为 0 或者取消事件，从而达到“静音”原声的效果
                event.setCanceled(true);
            }
        }
    }

    // 2. 在玩家受伤时主动播放猫叫声
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && hasCatAmulet(player)) {
            // 确保只在服务端播放（会自动同步到客户端）
            Level level = player.level();
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CAT_HURT,
                        player.getSoundSource(),
                        1.0F, // 音量
                        1F  // 音调，稍微尖锐一点更像受击
                );
            }
        }
    }

    // 2. 免疫摔落
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (hasCatAmulet(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // 3. 潜行降低仇恨范围
    @SubscribeEvent
    public static void onVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && player.isCrouching() && hasCatAmulet(player)) {
            event.modifyVisibility(0.5D);
        }
    }

    // 4. 对鱼类/守卫者增伤
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player && hasCatAmulet(player)) {
            LivingEntity target = event.getEntity();
            if (target instanceof AbstractFish || target instanceof Guardian) {
                event.setAmount(event.getAmount() * 1.25F);
            }
        }
    }

    // 5. 增加鱼类掉落
    @SubscribeEvent
    public static void onLooting(LootingLevelEvent event) {
        DamageSource source = event.getDamageSource();
        if (source != null && source.getEntity() instanceof Player player && hasCatAmulet(player)) {
            if (event.getEntity() instanceof AbstractFish || event.getEntity() instanceof Guardian) {
                event.setLootingLevel(event.getLootingLevel() + 2);
            }
        }
    }

    // 6. 苦力怕逃跑 AI 注入
    // 6. 苦力怕逃跑 AI 注入
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Creeper creeper) {
            // 显式指定 Predicate 类型，并放在参数列表的最后
            java.util.function.Predicate<LivingEntity> p = (target) -> {
                return target instanceof Player player && hasCatAmulet(player);
            };

            creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>(
                    creeper,
                    Player.class,
                    6.0F,    // 躲避距离
                    1.0D,    // 走路速度
                    1.2D,    // 冲刺速度
                    p        // 判定谓词放在最后
            ));
        }
    }

    // 7. 幻翼/苦力怕防止锁定目标
    @SubscribeEvent
    public static void onTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && hasCatAmulet(player)) {
            if (event.getEntity() instanceof Creeper || event.getEntity() instanceof Phantom) {
                event.setNewTarget(null); // 注意：ChangeTargetEvent 建议用 setNewTarget(null) 而非 setCanceled
            }
        }
    }
}