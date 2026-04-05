package com.first.themasses.events.mysterious;

import com.first.themasses.TheMasses;
import com.first.themasses.effect.ModEffects;
import com.first.themasses.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.atomic.AtomicInteger;

@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class Laowangispig {

    private static final String PIG_NAME = "laowang237";
    private static final String LAOWANG_TAG = "IsLaowangPig"; // 统一 NBT Key
    private static final double SEARCH_RADIUS = 3.0;
    private static final int MAX_CONVERSION_COUNT = 64;

    /**
     * 核心逻辑：闪电击中特定食物转化成“老王猪”
     */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = event.getLevel();

        if (!(entity instanceof LightningBolt lightning) || level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        final AtomicInteger pigsSpawnedCount = new AtomicInteger(0);

        serverLevel.getEntitiesOfClass(ItemEntity.class,
                        lightning.getBoundingBox().inflate(SEARCH_RADIUS),
                        itemEntity -> itemEntity.getItem().is(ModItems.LAOWANG237_FOOD.get()))
                .forEach(itemEntity -> {
                    if (pigsSpawnedCount.get() >= MAX_CONVERSION_COUNT) return;

                    int itemCount = itemEntity.getItem().getCount();
                    int remainingLimit = MAX_CONVERSION_COUNT - pigsSpawnedCount.get();
                    int pigsToSpawn = Math.min(itemCount, remainingLimit);

                    itemEntity.discard(); // 销毁掉落物

                    for (int i = 0; i < pigsToSpawn; i++) {
                        Pig pig = EntityType.PIG.create(serverLevel);
                        if (pig != null) {
                            pig.moveTo(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(),
                                    serverLevel.random.nextFloat() * 360F, 0);

                            // 设置名称与特殊 NBT 标记
                            pig.setCustomName(Component.literal(PIG_NAME));
                            pig.setCustomNameVisible(true);
                            pig.getPersistentData().putBoolean(LAOWANG_TAG, true); // 写入标记

                            serverLevel.addFreshEntity(pig);
                            pigsSpawnedCount.incrementAndGet();
                        }
                    }
                });
    }

    /**
     * 💥 重要补充：取代 Mixin，防止标记过的猪被雷劈变成猪灵
     */
    @SubscribeEvent
    public static void onPigStruckByLightning(EntityStruckByLightningEvent event) {
        if (event.getEntity() instanceof Pig pig) {
            // 只有带有我们标记的猪才免疫转化
            if (pig.getPersistentData().getBoolean(LAOWANG_TAG)) {
                // 取消事件，闪电将不再对这头猪造成任何原版效果（包括变异）
                event.setCanceled(true);
            }
        }
    }

    /**
     * 效果拦截逻辑
     */
    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player) {
            if (event.getEffectInstance().getEffect() == ModEffects.THINKING_QUIETLY.get()) {
                if (player.hasEffect(ModEffects.ONE_IDEA.get())) {
                    event.setResult(Event.Result.DENY);
                    player.getPersistentData().putInt("laowang_stand_ticks", 0);
                }
            }
        }
    }
}