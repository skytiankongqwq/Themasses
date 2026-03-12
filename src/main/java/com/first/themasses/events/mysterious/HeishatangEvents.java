package com.first.themasses.events.mysterious;

import com.first.themasses.TheMasses;
import com.first.themasses.curios.mysterious.heishatang;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
//黑砂糖饰品贿赂逻辑
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class HeishatangEvents {

    private static final UUID LUCK_ID = UUID.fromString("6a8b9c1d-1e2f-3a4b-5c6d-7e8f9a0b1c2d");

    // --- 1. 自动冶炼逻辑 ---
    @SubscribeEvent
    public static void handleAutoSmelt(BlockEvent.BreakEvent event) {
        Player player = event.getPlayer();
        if (player.level().isClientSide || player.isCreative()) return;

        CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).ifPresent(slot -> {
            if (slot.stack().getOrCreateTag().getInt("PowerCoal") < 100) return;

            ServerLevel level = (ServerLevel) player.level();
            LootParams.Builder paramsBuilder = new LootParams.Builder(level)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(event.getPos()))
                    .withParameter(LootContextParams.TOOL, player.getMainHandItem())
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                    .withOptionalParameter(LootContextParams.BLOCK_ENTITY, level.getBlockEntity(event.getPos()));

            List<ItemStack> originalDrops = event.getState().getDrops(paramsBuilder);
            boolean smeltedAny = false;

            for (ItemStack drop : originalDrops) {
                Optional<SmeltingRecipe> recipe = level.getRecipeManager()
                        .getRecipeFor(RecipeType.SMELTING, new SimpleContainer(drop), level);

                if (recipe.isPresent()) {
                    ItemStack result = recipe.get().getResultItem(level.registryAccess()).copy();
                    if (!result.isEmpty()) {
                        result.setCount(drop.getCount());
                        ItemEntity itemEntity = new ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, result);
                        level.addFreshEntity(itemEntity);
                        smeltedAny = true;
                    }
                } else {
                    ItemEntity itemEntity = new ItemEntity(level, event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5, drop);
                    level.addFreshEntity(itemEntity);
                }
            }

            if (smeltedAny) {
                level.removeBlock(event.getPos(), false);
                event.setCanceled(true);
            }
        });
    }

    // --- 2. 抢夺逻辑 ---
    @SubscribeEvent
    public static void handleLootingLevel(LootingLevelEvent event) {
        if (event.getEntity() instanceof Player player) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).ifPresent(slot -> {
                int bonus = Math.min(100, slot.stack().getOrCreateTag().getInt("PowerDiamond") / 32);
                event.setLootingLevel(event.getLootingLevel() + bonus);
            });
        }
    }

    // R键主动能力
    public static void executeHeishatangAbility(Player player, LivingEntity target) {
        if (player.level().isClientSide) return;

        CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).ifPresent(slot -> {
            ItemStack curioStack = slot.stack();
            CompoundTag curioNbt = curioStack.getOrCreateTag();
            long time = player.level().getGameTime();
            CompoundTag targetNbt = target.getPersistentData();

            // 1. 检查效果是否正在持续 (防止二次增加时间)
            boolean isUnderEffect = false;
            if (target instanceof Enemy && targetNbt.getLong("HeishatangNeutralEnd") > time) isUnderEffect = true;
            else if (target instanceof Villager && targetNbt.getLong("HeishatangVillagerEnd") > time) isUnderEffect = true;
            else if (targetNbt.getLong("HeishatangFollowEnd") > time) isUnderEffect = true;

            if (isUnderEffect) {
                player.displayClientMessage(Component.literal("目标正处于能力影响中，无法叠加时间！").withStyle(ChatFormatting.YELLOW), true);
                return;
            }

            // 2. 检查冷却时间 (结束后才开始计时的 CD)
            long cdEnd = targetNbt.getLong("HeishatangGlobalCD");
            if (time < cdEnd) {
                long remainingSec = (cdEnd - time) / 20;
                player.displayClientMessage(Component.literal("目标正处于冷却中，剩余: " + remainingSec + "秒").withStyle(ChatFormatting.RED), true);
                return;
            }

            // --- 3. 消耗与模式判定 ---
            int mode = curioNbt.getInt("UseMode");
            int cost = (mode == 1) ? 9 : 1;
            int currentEnergy = curioNbt.getInt("EmeraldEquivalent");

            if (currentEnergy < cost) {
                player.displayClientMessage(Component.literal("储存能量不足！需要: " + cost).withStyle(ChatFormatting.RED), true);
                return;
            }

            // --- 4. 执行效果并设置结束时间与冷却 ---
            curioNbt.putInt("EmeraldEquivalent", currentEnergy - cost);

            int cdTicks = (mode == 1) ? 800 : 400;
            int durationTicks = 0;

            if (target instanceof Enemy) {
                durationTicks = (mode == 1 ? 500 : 100);
                targetNbt.putLong("HeishatangNeutralEnd", time + durationTicks);

                if (target instanceof Mob mob) {
                    // 1. 无论是否是苦力怕，先清除仇恨目标使其变为中立
                    mob.setTarget(null);
                    mob.setLastHurtByMob(null);

                    // 2. 针对苦力怕的特殊熄火处理
                    if (mob instanceof Creeper creeper) {
                        // 强制设置膨胀方向为收缩状态
                        creeper.setSwellDir(-1);
                        // 停止当前导航，防止 AI 立即重新锁定
                        creeper.getNavigation().stop();

                        try {
                            // 使用反射重置点燃状态 (ignited) 和 膨胀进度 (swell)
                            // f_32270_ -> ignited (Boolean)
                            // f_32271_ -> swell (Integer)

                            // 获取数据同步项并设为 false (熄灭闪烁)
                            net.minecraft.network.syncher.EntityDataAccessor<Boolean> ignitedData =
                                    ObfuscationReflectionHelper.getPrivateValue(Creeper.class, null, "f_32270_");
                            if (ignitedData != null) {
                                creeper.getEntityData().set(ignitedData, false);
                            }

                            // 强行将膨胀数值归零 (防止瞬爆)
                            ObfuscationReflectionHelper.setPrivateValue(Creeper.class, creeper, 0, "f_32271_");

                        } catch (Exception e) {
                            // 开发环境/不同版本的兼容 fallback
                            try {
                                ObfuscationReflectionHelper.setPrivateValue(Creeper.class, creeper, 0, "swell");
                                // 如果字段不可直接访问，尝试通过 NBT 强制覆盖
                                creeper.getPersistentData().putInt("Swell", 0);
                            } catch (Exception ignored) {}
                        }
                    }
                }

                playEffect((ServerLevel)player.level(), target, ParticleTypes.HAPPY_VILLAGER);
                player.displayClientMessage(Component.literal("成功安抚目标").withStyle(ChatFormatting.GREEN), true);
            } else if (target instanceof Villager villager) {
                durationTicks = 400;
                targetNbt.putLong("HeishatangVillagerEnd", time + durationTicks);
                for (MerchantOffer offer : villager.getOffers()) {
                    offer.resetUses();
                    try { ObfuscationReflectionHelper.setPrivateValue(MerchantOffer.class, offer, 9999, "maxUses"); } catch (Exception ignored) {}
                }
                playEffect((ServerLevel)player.level(), target, ParticleTypes.HAPPY_VILLAGER);
            } else if (target instanceof Animal || target instanceof net.minecraft.world.entity.animal.IronGolem) {
                durationTicks = (mode == 1 ? 3600 : 600);
                targetNbt.putLong("HeishatangFollowEnd", time + durationTicks);
                targetNbt.putUUID("HeishatangOwner", player.getUUID());
                playEffect((ServerLevel)player.level(), target, ParticleTypes.HEART);
            }
            targetNbt.putLong("HeishatangGlobalCD", time + durationTicks + cdTicks);
            player.displayClientMessage(Component.literal("技能发动成功！").withStyle(ChatFormatting.GREEN), true);
        });
    }

    // --- 4. 战斗与 Tick 逻辑 ---
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).ifPresent(slot -> {
                if (slot.stack().getOrCreateTag().getInt("PowerRedstone") >= 500) event.getEntity().setSecondsOnFire(5);
            });
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;
        long time = entity.level().getGameTime();

        if (entity instanceof Player player) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).ifPresent(slot -> {
                CompoundTag nbt = slot.stack().getOrCreateTag();

                // 自动修复
                if (time % 20 == 0) {
                    int r = nbt.getInt("PowerIron") / 10;
                    if (r > 0 && player.getMainHandItem().isDamaged()) player.getMainHandItem().setDamageValue(Math.max(0, player.getMainHandItem().getDamageValue() - r));
                }

                // 幸运属性
                AttributeInstance luck = player.getAttribute(Attributes.LUCK);
                if (luck != null) {
                    double val = nbt.getInt("PowerGold") * 0.02;
                    if (luck.getModifier(LUCK_ID) == null || luck.getModifier(LUCK_ID).getAmount() != val) {
                        luck.removeModifier(LUCK_ID);
                        luck.addTransientModifier(new AttributeModifier(LUCK_ID, "HeishatangLuck", val, AttributeModifier.Operation.ADDITION));
                    }
                }

                // 青金石急迫状态
                int l = nbt.getInt("PowerLapis") / 160;
                if (l > 0) {
                    MobEffectInstance activeEffect = player.getEffect(MobEffects.DIG_SPEED);
                    if (activeEffect == null || activeEffect.getDuration() <= 380) {
                        player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 400, Math.min(99, l - 1), false, false, false));
                    }
                }

                // 抗火状态
                if (nbt.getInt("PowerNetherite") >= 9) {
                    MobEffectInstance activeFire = player.getEffect(MobEffects.FIRE_RESISTANCE);
                    if (activeFire == null || activeFire.getDuration() <= 380) {
                        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 400, 0, false, false, false));
                    }
                }
            });
        }
        handleRidingLogic(entity, entity.getPersistentData(), time);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        Level level = event.getLevel();

        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (level.isClientSide) return;

        if (player.isShiftKeyDown()) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).ifPresent(slot -> {
                CompoundTag tag = slot.stack().getOrCreateTag();
                int current = tag.getInt("UseMode");
                int next = (current == 0) ? 1 : 0;
                tag.putInt("UseMode", next);

                String modeName = (next == 1 ? "【绿宝石块】(消耗9能量)" : "【绿宝石】(消耗1能量)");
                player.displayClientMessage(Component.literal("能力模式切换 -> " + modeName).withStyle(ChatFormatting.GOLD), true);
            });
            event.setCanceled(true);
            return;
        }

        CompoundTag nbt = event.getTarget().getPersistentData();
        if (nbt.getLong("HeishatangFollowEnd") > level.getGameTime()) {
            if (player.getUUID().equals(nbt.getUUID("HeishatangOwner"))) player.startRiding(event.getTarget());
        }
    }

    private static void handleRidingLogic(LivingEntity entity, CompoundTag nbt, long time) {
        if (entity instanceof Mob mob) {
            long end = nbt.getLong("HeishatangFollowEnd");

            if (end != 0 && end <= time) {
                if (mob.isVehicle()) mob.ejectPassengers();
                // 如果 setMaxUpStep 报错，可改为 mob.setStepHeight(0.6F)
                mob.setMaxUpStep(0.6F);
                nbt.remove("HeishatangFollowEnd");
                return;
            }

            if (end > time && mob.getFirstPassenger() instanceof Player c) {
                mob.setYRot(c.getYRot());
                mob.setYHeadRot(c.getYRot());
                mob.yRotO = mob.getYRot();
                mob.setMaxUpStep(1.2F);

                float f = c.zza;
                float s = c.xxa;

                if (f != 0 || s != 0) {
                    mob.setSpeed(0.3F);
                    mob.travel(new Vec3(s, 0, f));
                }
            } else {
                mob.setMaxUpStep(0.6F);
            }
        }
    }

    private static void playEffect(ServerLevel l, LivingEntity e, net.minecraft.core.particles.ParticleOptions p) {
        l.sendParticles(p, e.getX(), e.getY() + 1.2, e.getZ(), 8, 0.2, 0.2, 0.2, 0.05);
    }
}