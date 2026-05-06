package com.first.themasses.events.mysterious;

import com.first.themasses.curios.mysterious.Auviotre;
import com.first.themasses.entity.SmallFireBall;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
@SuppressWarnings("removal")
// auv饰品事件
@Mod.EventBusSubscriber(modid = "themasses")
public class AuviotreEvents {

    /**
     * 整合后的伤害处理逻辑
     * 1. 玩家受攻击：计算减伤、护盾、以及发光状态下的10%减免
     * 2. 玩家攻击：白天伤害降低，夜晚使目标发光，且对发光生物增伤50%，并概率触发火球
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        Entity attackerEntity = event.getSource().getEntity();
        Entity directSource = event.getSource().getDirectEntity();

        // --- 逻辑 A: 处理受攻击者的防御 ---
        CuriosApi.getCuriosHelper().findFirstCurio(victim, item -> item.getItem() instanceof Auviotre).ifPresent(slotResult -> {
            float finalDamage = Auviotre.handleDamage(victim, slotResult.stack(), event.getAmount());
            event.setAmount(finalDamage);
        });

        // --- 逻辑 B: 处理攻击者的进攻 ---
        if (attackerEntity instanceof LivingEntity attacker) {
            CuriosApi.getCuriosHelper().findFirstCurio(attacker, item -> item.getItem() instanceof Auviotre).ifPresent(slotResult -> {

                // 1. 处理发光增伤/白天减伤
                float modifiedDamage = Auviotre.handleAttack(attacker, victim, event.getAmount());
                event.setAmount(modifiedDamage);

                // 2火球触发逻辑
                if (attacker instanceof Player player && !level.isClientSide) {
                    if (!(directSource instanceof SmallFireBall)) {
                        if (player.getRandom().nextFloat() < 0.7F) {
                            SmallFireBall fireBall = new SmallFireBall(level, player, victim);
                            level.addFreshEntity(fireBall);

                            level.playSound(null, victim.getX(), victim.getY() + 5, victim.getZ(),
                                    SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8F, 1.5F);
                        }
                    }
                }
            });
        }
    }

    /**
     * 击杀生物逻辑：60% 概率掉落 1-3 个紫水晶碎片
     */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity killerEntity = event.getSource().getEntity();

        if (killerEntity instanceof LivingEntity killer) {
            CuriosApi.getCuriosHelper().findFirstCurio(killer, item -> item.getItem() instanceof Auviotre).ifPresent(slotResult -> {
                LivingEntity victim = event.getEntity();
                Level level = victim.level();

                if (!level.isClientSide && level.random.nextFloat() < 0.60F) {
                    int count = 1 + level.random.nextInt(3); // 1-3个
                    victim.spawnAtLocation(new ItemStack(Items.AMETHYST_SHARD, count));
                }
            });
        }

        // 原有的死亡重置护盾逻辑 (针对佩戴饰品的玩家死亡)
        if (event.getEntity() instanceof Player player) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, item -> item.getItem() instanceof Auviotre).ifPresent(slotResult -> {
                Auviotre.setShield(slotResult.stack(), 20.0F);
            });
        }
    }

    /**
     * 紫水晶碎片充能逻辑 (支持穿在身上时右键充能)
     */
    @SubscribeEvent
    public static void onRightClickWithShard(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack heldItem = event.getItemStack();
        Level level = player.level();

        if (heldItem.is(Items.AMETHYST_SHARD)) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, item -> item.getItem() instanceof Auviotre).ifPresent(slotResult -> {
                ItemStack curioStack = slotResult.stack();
                float shield = Auviotre.getShield(curioStack);

                if (shield < 20.0F) {
                    if (!level.isClientSide) {
                        Auviotre.setShield(curioStack, shield + 1.0F);
                        if (!player.getAbilities().instabuild) {
                            heldItem.shrink(1);
                        }
                        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.2F);
                    }
                    event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
                    event.setCanceled(true);
                }
            });
        }
    }

    /**
     * 摔落逻辑：免疫摔伤 + 5格以上落地触发冲击波
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        LivingEntity entity = event.getEntity();

        CuriosApi.getCuriosHelper().findFirstCurio(entity, item -> item.getItem() instanceof Auviotre).ifPresent(slotResult -> {
            float distance = event.getDistance();

            if (distance > 5.0F) {
                float splashDamage = (distance - 5.0F) * 2.0F;
                AABB searchArea = entity.getBoundingBox().inflate(5.0D);
                List<LivingEntity> targets = entity.level().getEntitiesOfClass(LivingEntity.class, searchArea);

                for (LivingEntity target : targets) {
                    if (target != entity) {
                        target.hurt(entity.damageSources().fall(), splashDamage);
                    }
                }
                // 落地特效音
                entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                        SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 0.5F);
            }

            event.setDamageMultiplier(0);
            event.setCanceled(true);
        });
    }

    /**
     * 放置方块检测
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player player) {
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            ItemStack placedStack = ItemStack.EMPTY;
            if (mainHand.getItem() instanceof Auviotre) {
                placedStack = mainHand;
            } else if (offHand.getItem() instanceof Auviotre) {
                placedStack = offHand;
            }

            if (!placedStack.isEmpty()) {
            }
        }
    }

    /**
     * 辅助判定：检查实体是否受饰品增强
     */
    public static boolean hasAuviotreBoost(LivingEntity entity) {
        return CuriosApi.getCuriosHelper().findFirstCurio(entity, item -> item.getItem() instanceof Auviotre).isPresent();
    }
}