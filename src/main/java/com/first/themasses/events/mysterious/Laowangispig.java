package com.first.themasses.events.mysterious;

import com.first.themasses.TheMasses;

import com.first.themasses.effect.ModEffects;
import com.first.themasses.item.ModItems;

// 假设你的物品注册类是 ModItems

// 💥 导入新的事件类：EntityJoinLevelEvent
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 导入必要的 Minecraft 类
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import static net.minecraftforge.event.entity.living.MobEffectEvent.*;

//laowang237_food转化
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class Laowangispig {

    private static final String PIG_NAME = "laowang237";
    private static final double SEARCH_RADIUS = 3.0;
    private static final int MAX_CONVERSION_COUNT = 64;

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();

        // 1. 检查实体是否是闪电，且只在服务器端执行
        if (!(entity instanceof LightningBolt lightning) || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // 💥 关键修改：使用原子整数来追踪当前已转化的总数（因为 forEach 内部是 lambda 表达式）
        final java.util.concurrent.atomic.AtomicInteger pigsSpawnedCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // 2. 在闪电实体的周围搜索目标掉落物
        serverLevel.getEntitiesOfClass(ItemEntity.class,
                        lightning.getBoundingBox().inflate(SEARCH_RADIUS),
                        // 3. 过滤器：检查 ItemEntity 掉落的物品是否是 Laowang Food
                        itemEntity -> itemEntity.getItem().is(ModItems.LAOWANG237_FOOD.get()))
                .forEach(itemEntity -> {
                    // 检查是否已达到上限
                    if (pigsSpawnedCount.get() >= MAX_CONVERSION_COUNT) {
                        return; // 跳过处理当前掉落物
                    }

                    int itemCount = itemEntity.getItem().getCount();

                    // 💥 限制生成数量：计算剩余可生成数量
                    int remainingLimit = MAX_CONVERSION_COUNT - pigsSpawnedCount.get();
                    int pigsToSpawn = Math.min(itemCount, remainingLimit);

                    // 4. 移除掉落物实体
                    itemEntity.discard();

                    // 5. 在掉落物的位置循环生成相应数量的猪 (最多 pigsToSpawn 只)
                    for (int i = 0; i < pigsToSpawn; i++) {
                        Pig pig = EntityType.PIG.create(serverLevel);
                        if (pig != null) {
                            pig.moveTo(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                                    pig.getYRot(), pig.getXRot());

                            // 6. 设置猪的名称
                            pig.setCustomName(Component.literal(PIG_NAME));
                            pig.setCustomNameVisible(true);
                            pig.getPersistentData().putBoolean("IsLaowangPig", true);

                            serverLevel.addFreshEntity(pig);
                            // 💥 更新已生成计数
                            pigsSpawnedCount.incrementAndGet();
                        }
                    }
                });
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player) {
            // 1. 检查准备应用的效果是否是 THINKING_QUIETLY
            if (event.getEffectInstance().getEffect() == ModEffects.THINKING_QUIETLY.get()) {
                // 2. 判定条件：如果玩家身上已经存在 ONE_IDEA 效果
                if (player.hasEffect(ModEffects.ONE_IDEA.get())) {
                    // 3. 彻底拦截：不允许获得该效果
                    event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                    // 4. 重置老王的计时器（防止 NBT 数值残留导致下次触发跳变）
                    player.getPersistentData().putInt("laowang_stand_ticks", 0);
                }
            }
        }
    }
}