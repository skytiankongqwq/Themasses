package com.first.themasses.events.prop;

import com.first.themasses.Client.miku.MikuManager;
import com.first.themasses.Client.miku.MikuPawManager;
import com.first.themasses.Client.miku.MikuSupremeManager;
import com.first.themasses.Config;
import com.first.themasses.attributes.RegistryAttribute;
import com.first.themasses.item.prop.MikuLaMeowBlessingItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class MikuLaMeowEvents {

    private static final UUID MIKU_BUFF_ID = UUID.fromString("6a17b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c");

    private static Attribute getMikuSizeAttr() {
        return RegistryAttribute.SCALE.get();
    }

    private static boolean isFriendly(LivingEntity target, Player player) {
        if (target == player) return true;
        return target.isAlliedTo(player);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof MikuLaMeowBlessingItem && event.getTarget() instanceof LivingEntity target) {
            if (player.getCooldowns().isOnCooldown(stack.getItem())) return;
            if (!isFriendly(target, player)) return;

            int cdUp = stack.getOrCreateTag().getInt("CooldownUp");

            if (player.isCrouching()) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 4.0F, 1.2F);
                if (!player.level().isClientSide) {
                    applySingleBlessing(target, stack);
                    player.getCooldowns().addCooldown(stack.getItem(), 800 - (cdUp * 100));
                }
            } else {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CAT_AMBIENT, SoundSource.PLAYERS, 4.0F, 1.0F);
                if (!player.level().isClientSide) {
                    triggerAreaBlessing(player, player.level(), stack);
                    player.getCooldowns().addCooldown(stack.getItem(), 600 - (cdUp * 100));
                }
            }
            event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();

        if (stack.getItem() instanceof MikuLaMeowBlessingItem) {
            if (player.getCooldowns().isOnCooldown(stack.getItem())) return;

            if (!player.isCrouching()) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.CAT_PURREOW, SoundSource.PLAYERS, 4.0F, 1.0F);
                if (!player.level().isClientSide) {
                    triggerAreaBlessing(player, player.level(), stack);
                    int cdUp = stack.getOrCreateTag().getInt("CooldownUp");
                    player.getCooldowns().addCooldown(stack.getItem(), 600 - (cdUp * 100));
                }
                event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
                event.setCanceled(true);
            }
        }
    }

    public static void triggerAreaBlessing(Player player, Level world, ItemStack stack) {
        int upgradeLevel = stack.getOrCreateTag().getInt("RangeUpgrade");
        double radius = 12.0D + (upgradeLevel * 2.0D);

        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && isFriendly(e, player));
        for (LivingEntity target : targets) {
            applyAreaBlessing(target, stack);
        }
    }

    public static void applySingleBlessing(LivingEntity target, ItemStack stack) {
        // 适配修改：使用 MikuPawManager 激活单体猫爪特效渲染
        MikuPawManager.activate(target, 3);
        if (!target.level().isClientSide) {
            long activateTime = target.level().getGameTime() + 50;
            CompoundTag data = target.getPersistentData();
            data.putLong("MikuBuffActivateTime_Single", activateTime);
            data.putInt("PendingUpgradeLevel", stack.getOrCreateTag().getInt("UpgradeLevel"));
            data.putInt("PendingDurationUp", stack.getOrCreateTag().getInt("DurationUp"));
        }
    }

    public static void applyAreaBlessing(LivingEntity target, ItemStack stack) {
        // 适配修改：使用 MikuPawManager 激活群体特效渲染（或根据需要使用 MikuManager）
        MikuPawManager.activate(target, 3);
        if (!target.level().isClientSide) {
            long activateTime = target.level().getGameTime() + 50;
            CompoundTag data = target.getPersistentData();
            data.putLong("MikuBuffActivateTime_Area", activateTime);
            data.putInt("PendingUpgradeLevel", stack.getOrCreateTag().getInt("UpgradeLevel"));
            data.putInt("PendingDurationUp", stack.getOrCreateTag().getInt("DurationUp"));
        }
    }

    private static void applyCommonEffects(LivingEntity target, int duration, int levelAmp, int upLevel, boolean isShiftRightClick) {
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, levelAmp, false, false, true));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, levelAmp, false, false, true));

        if (target.isInvertedHealAndHarm()) {
            addEffectIfPresent(target, "goety:necrosis", duration, levelAmp);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, levelAmp, false, false, true));
        }

        int asylumAmp = isShiftRightClick ? 1 : 0;
        addEffectIfPresent(target, "more_potion_effects:asylum", duration, asylumAmp);
        addEffectIfPresent(target, "goety:deflective", duration, Math.min(2, levelAmp));

        ResourceLocation loc = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (loc != null) {
            String entityId = loc.toString();

            int archerLvl = -1;
            if (Config.ACCURATE_LVL7_ENTITIES.get().contains(entityId)) archerLvl = Math.max(archerLvl, upLevel > 0 ? 6 : 5);
            if (Config.ACCURATE_LVL5_ENTITIES.get().contains(entityId)) archerLvl = Math.max(archerLvl, upLevel > 0 ? 4 : 3);
            if (Config.ACCURATE_LVL4_ENTITIES.get().contains(entityId)) archerLvl = Math.max(archerLvl, upLevel > 0 ? 3 : 2);
            if (Config.ACCURATE_LVL3_ENTITIES.get().contains(entityId)) archerLvl = Math.max(archerLvl, upLevel > 0 ? 2 : 1);
            if (Config.ACCURATE_ENTITIES.get().contains(entityId)) archerLvl = Math.max(archerLvl, Math.min(1, upLevel));

            if (archerLvl != -1) {
                addEffectIfPresent(target, "more_potion_effects:accurate", duration, archerLvl);
            }

            int magicLvl = -1;
            if (Config.MAGIC_FOCUS_LVL3_ENTITIES.get().contains(entityId)) magicLvl = Math.max(magicLvl, upLevel > 0 ? 2 : 1);
            if (Config.MAGIC_FOCUS_ENTITIES.get().contains(entityId)) magicLvl = Math.max(magicLvl, Math.min(1, upLevel));

            if (magicLvl != -1) {
                addEffectIfPresent(target, "more_potion_effects:magic_focus", duration, magicLvl);
            }
        }

        if (isShiftRightClick) {
            addEffectIfPresent(target, "goety:shielding", duration, Math.min(3, levelAmp));
            addEffectIfPresent(target, "goety:rallying", duration, Math.min(1, levelAmp));
        }
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
            long currentTime = entity.level().getGameTime();
            CompoundTag data = entity.getPersistentData();

            if (data.contains("MikuBuffActivateTime_Single")) {
                long activateTime = data.getLong("MikuBuffActivateTime_Single");
                if (currentTime >= activateTime) {
                    data.remove("MikuBuffActivateTime_Single");
                    MikuManager.deactivate(entity); // 单体激活时，临时关闭群体动画
                    applyDelayedSingleBlessing(entity, data);
                }
            }

            if (data.contains("MikuBuffActivateTime_Area")) {
                long activateTime = data.getLong("MikuBuffActivateTime_Area");
                if (currentTime >= activateTime) {
                    data.remove("MikuBuffActivateTime_Area");
                    boolean isSingleActive = data.contains("MikuBuffEnd_Single") && currentTime <= data.getLong("MikuBuffEnd_Single");

                    if (isSingleActive) {
                        applyDelayedSingleBlessing(entity, data);
                    } else {
                        applyDelayedAreaBlessing(entity, data);
                    }
                }
            }

            boolean hasSingle = data.contains("MikuBuffEnd_Single");
            boolean hasArea = data.contains("MikuBuffEnd_Area");
            boolean singleActive = hasSingle && currentTime <= data.getLong("MikuBuffEnd_Single");
            boolean areaActive = hasArea && currentTime <= data.getLong("MikuBuffEnd_Area");
            Attribute mikuSize = getMikuSizeAttr();

            // 单体 Buff 到期时的处理
            if (hasSingle && !singleActive) {
                data.remove("MikuBuffEnd_Single");
                MikuPawManager.deactivate(entity); // 确保服务端严格关闭单体动画

                if (areaActive) {
                    modifyMultiply(entity, Attributes.MAX_HEALTH, 0.25, MIKU_BUFF_ID);
                    modifyMultiply(entity, Attributes.ARMOR, 0.1, MIKU_BUFF_ID);
                    modifyMultiply(entity, Attributes.ATTACK_DAMAGE, 0.1, MIKU_BUFF_ID);
                    modifyMultiply(entity, mikuSize, 0.10, MIKU_BUFF_ID);
                    modifyMultiply(entity, Attributes.FLYING_SPEED, 0.25, MIKU_BUFF_ID);
                    modifyAdd(entity, Attributes.KNOCKBACK_RESISTANCE, 0.2, MIKU_BUFF_ID);

                    applyIronSpellbooksAttributes(entity, 0.50, 0.50);

                    removeModifier(entity, Attributes.MOVEMENT_SPEED, MIKU_BUFF_ID);
                    removeModifier(entity, Attributes.ARMOR_TOUGHNESS, MIKU_BUFF_ID);
                    removeModifier(entity, mikuSize, MIKU_BUFF_ID);

                    // 【动画修复】如果单体结束时群体 Buff 还在，重新激活被单体覆盖掉的群体动画
                    long remainingAreaTicks = data.getLong("MikuBuffEnd_Area") - currentTime;
                    if (remainingAreaTicks > 0) {
                        MikuManager.activate(entity, (int) (remainingAreaTicks / 20));
                    }
                }
            }

            // 群体 Buff 到期时的处理
            if (hasArea && !areaActive) {
                data.remove("MikuBuffEnd_Area");
                MikuManager.deactivate(entity); // 确保服务端严格关闭群体动画
            }

            // 彻底没有任何 Buff 时的处理
            if (!singleActive && !areaActive && (hasSingle || hasArea)) {
                // 保底机制：双重确认关闭所有动画状态，防止 NBT 残留
                MikuPawManager.deactivate(entity);
                MikuManager.deactivate(entity);
                MikuSupremeManager.deactivate(entity);

                removeModifier(entity, Attributes.MAX_HEALTH, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.MOVEMENT_SPEED, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.ARMOR, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.ATTACK_DAMAGE, MIKU_BUFF_ID);
                removeModifier(entity, mikuSize, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.ARMOR_TOUGHNESS, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.FLYING_SPEED, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.KNOCKBACK_RESISTANCE, MIKU_BUFF_ID);
                removeIronSpellbooksAttributes(entity);
            }
        }
    }

    private static void applyDelayedSingleBlessing(LivingEntity target, CompoundTag data) {
        float oldMax = target.getMaxHealth();
        int upLevel = data.getInt("PendingUpgradeLevel");
        int durUp = data.getInt("PendingDurationUp");
        int singleLevel = 1 + upLevel;
        int duration = 400 + (durUp * 200);

        applyCommonEffects(target, duration, singleLevel, upLevel, true);

        float step = 1.0F + (singleLevel * 0.1F);

        modifyMultiply(target, Attributes.MAX_HEALTH, 0.5, MIKU_BUFF_ID);
        modifyMultiply(target, Attributes.MOVEMENT_SPEED, 0.2 * step, MIKU_BUFF_ID);
        modifyMultiply(target, Attributes.ARMOR, 0.2 * step, MIKU_BUFF_ID);
        modifyMultiply(target, Attributes.ATTACK_DAMAGE, 0.25 * step, MIKU_BUFF_ID);
        modifyMultiply(target, Attributes.ARMOR_TOUGHNESS, 0.25 * step, MIKU_BUFF_ID);
        modifyMultiply(target, getMikuSizeAttr(), 0.25, MIKU_BUFF_ID);
        modifyMultiply(target, Attributes.FLYING_SPEED, 0.5, MIKU_BUFF_ID);
        modifyAdd(target, Attributes.KNOCKBACK_RESISTANCE, 0.4, MIKU_BUFF_ID);

        applyIronSpellbooksAttributes(target, 1.0, 1.0);

        float addedHealth = target.getMaxHealth() - oldMax;
        if (addedHealth > 0) {
            target.setHealth(target.getHealth() + addedHealth);
        }

        target.getPersistentData().putLong("MikuBuffEnd_Single", target.level().getGameTime() + duration);
        MikuPawManager.activate(target, duration / 20); // 激活单体动画
    }

    private static void applyDelayedAreaBlessing(LivingEntity target, CompoundTag data) {
        int upLevel = data.getInt("PendingUpgradeLevel");
        int durUp = data.getInt("PendingDurationUp");
        float oldMax = target.getMaxHealth();
        int areaLevel = 0 + upLevel;
        int duration = 600 + (durUp * 200);

        applyCommonEffects(target, duration, areaLevel, upLevel, false);

        long currentTime = target.level().getGameTime();
        CompoundTag persistentData = target.getPersistentData();
        boolean singleActive = persistentData.contains("MikuBuffEnd_Single") && currentTime <= persistentData.getLong("MikuBuffEnd_Single");

        if (!singleActive) {
            modifyMultiply(target, Attributes.MAX_HEALTH, 0.25, MIKU_BUFF_ID);
            modifyMultiply(target, Attributes.ARMOR, 0.1, MIKU_BUFF_ID);
            modifyMultiply(target, Attributes.ATTACK_DAMAGE, 0.1, MIKU_BUFF_ID);
            modifyMultiply(target, getMikuSizeAttr(), 0.10, MIKU_BUFF_ID);
            modifyMultiply(target, Attributes.FLYING_SPEED, 0.25, MIKU_BUFF_ID);
            modifyAdd(target, Attributes.KNOCKBACK_RESISTANCE, 0.2, MIKU_BUFF_ID);

            applyIronSpellbooksAttributes(target, 0.50, 0.50);

            float addedHealth = target.getMaxHealth() - oldMax;
            if (addedHealth > 0) {
                target.setHealth(target.getHealth() + addedHealth);
            }
        }

        target.getPersistentData().putLong("MikuBuffEnd_Area", target.level().getGameTime() + duration);
        MikuManager.activate(target, duration / 20); // 激活群体动画
    }

    private static void applyIronSpellbooksAttributes(LivingEntity target, double powerAmp, double resistAmp) {
        ResourceLocation loc = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        if (loc != null && Config.IRON_SPELLBOOKS_ENTITIES.get().contains(loc.toString())) {
            Attribute spellPower = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks:spell_power"));
            Attribute spellResist = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks:spell_resistance"));

            if (spellPower != null) modifyMultiply(target, spellPower, powerAmp, MIKU_BUFF_ID);
            if (spellResist != null) modifyMultiply(target, spellResist, resistAmp, MIKU_BUFF_ID);
        }
    }

    private static void removeIronSpellbooksAttributes(LivingEntity target) {
        Attribute spellPower = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks:spell_power"));
        Attribute spellResist = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("irons_spellbooks:spell_resistance"));
        if (spellPower != null) removeModifier(target, spellPower, MIKU_BUFF_ID);
        if (spellResist != null) removeModifier(target, spellResist, MIKU_BUFF_ID);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        long currentTime = entity.level().getGameTime();
        CompoundTag data = entity.getPersistentData();
        if ((data.contains("MikuBuffEnd_Single") && currentTime <= data.getLong("MikuBuffEnd_Single")) ||
                (data.contains("MikuBuffEnd_Area") && currentTime <= data.getLong("MikuBuffEnd_Area"))) {
            event.setAmount(event.getAmount() * 0.9F);
        }
    }

    private static void modifyMultiply(LivingEntity e, Attribute a, double v, UUID u) {
        if (a == null) return;
        var i = e.getAttribute(a);
        if (i != null) {
            i.removeModifier(u);
            i.addTransientModifier(new AttributeModifier(u, "LaMeowBuff", v, AttributeModifier.Operation.MULTIPLY_TOTAL));
            if (a == getMikuSizeAttr()) e.refreshDimensions();
        }
    }

    private static void modifyAdd(LivingEntity e, Attribute a, double v, UUID u) {
        if (a == null) return;
        var i = e.getAttribute(a);
        if (i != null) {
            i.removeModifier(u);
            i.addTransientModifier(new AttributeModifier(u, "LaMeowBuff", v, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void removeModifier(LivingEntity e, Attribute a, UUID u) {
        if (a == null) return;
        var i = e.getAttribute(a);
        if (i != null) {
            i.removeModifier(u);
            if (a == getMikuSizeAttr()) e.refreshDimensions();
        }
    }

    private static void addEffectIfPresent(LivingEntity entity, String id, int duration, int amp) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
        if (effect != null) {
            entity.addEffect(new MobEffectInstance(effect, duration, amp, false, false, true));
        }
    }
}