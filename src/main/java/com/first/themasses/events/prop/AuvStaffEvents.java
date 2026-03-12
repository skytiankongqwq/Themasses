package com.first.themasses.events.prop;

import com.first.themasses.item.prop.AuvStaff;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber(modid = "themasses")
public class AuvStaffEvents {

    // 缓存反射方法：dropFromLootTable
    private static final Method DROP_FROM_LOOT_TABLE = ObfuscationReflectionHelper.findMethod(
            LivingEntity.class,
            "m_7625_", // 1.20.1 映射下的 SRG 名称
            net.minecraft.world.damagesource.DamageSource.class,
            boolean.class
    );
   //玩家死亡掉级
    @SubscribeEvent
    public static void onPlayerDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            int currentStage = player.getPersistentData().getInt("auv_staff_stage");

            if (currentStage > 0) {
                // 掉落一个阶段
                int newStage = currentStage - 1;
                player.getPersistentData().putInt("auv_staff_stage", newStage);
                player.getPersistentData().putInt("auv_staff_clicks", 0); // 重置点击数

                // 应用降级后的属性 (重新调用 applyUpgrade)
                AuvStaff.applyUpgrade(player, newStage);

                player.displayClientMessage(Component.literal("§6由于你的死亡，退化至 " + newStage + " 阶"), false);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // --- 逻辑 1: 永久锁定幼年体 ---
        if (entity.getPersistentData().getBoolean("auv_age_locked")) {
            if (entity instanceof AgeableMob ageable) {
                if (ageable.getAge() > -20) { // 接近成年时重置
                    ageable.setAge(-24000);
                }
            } else if (entity instanceof Zombie zombie) {
                if (!zombie.isBaby()) {
                    zombie.setBaby(true);
                }
            }
        }

        // --- 逻辑 2: 巨人僵尸倒计时消失 ---
        if (entity.getPersistentData().contains("auv_is_giant")) {
            int timer = entity.getPersistentData().getInt("auv_giant_timer");
            if (timer > 0) {
                entity.getPersistentData().putInt("auv_giant_timer", timer - 1);
            } else {
                entity.discard(); // 时间到，销毁巨人
            }
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;

        // --- 逻辑 1: 受到伤害重置巨人消失计时 ---
        if (victim.getPersistentData().contains("auv_is_giant")) {
            victim.getPersistentData().putInt("auv_giant_timer", 100); // 重置为 5 秒
        }

        // --- 逻辑 2: 动态概率触发掉落 ---
        if (victim.getPersistentData().getBoolean("auv_staff_affected")) {
            // 从 NBT 读取在法杖类中设定的概率 (0.1, 0.3, 0.7)
            float chance = victim.getPersistentData().getFloat("auv_drop_chance");

            // --- 概率修正: 如果是 BOSS，掉落概率减半 ---
            if (victim.getType().is(Tags.EntityTypes.BOSSES)) {
                chance /= 2.0F;
            }

            // 执行随机判定
            if (victim.getRandom().nextFloat() < chance) {
                try {
                    // 调用受保护的 dropFromLootTable 方法强制掉落物品
                    DROP_FROM_LOOT_TABLE.invoke(victim, event.getSource(), true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}