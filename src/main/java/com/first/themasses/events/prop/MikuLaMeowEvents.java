package com.first.themasses.events.prop;

import com.core.dream_sakura.attributes.RegistryAttribute;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = "themasses")
public class MikuLaMeowEvents {

    private static final UUID MIKU_BUFF_ID = UUID.fromString("6a17b2c3-4d5e-6f7a-8b9c-0d1e2f3a4b5c");

    // 辅助方法：现在指向 RegistryAttribute.SCALE
    public static Attribute getMikuSizeAttr() {
        return RegistryAttribute.SCALE.get();
    }

//    @Mod.EventBusSubscriber(modid = "themasses", value = Dist.CLIENT)
//    public static class ClientEvents {
//        @SubscribeEvent
//        public static void onRenderPre(RenderLivingEvent.Pre<?, ?> event) {
//            Attribute mikuSize = getMikuSizeAttr();
//            var inst = event.getEntity().getAttribute(mikuSize);
//            if (inst != null && inst.getValue() != 1.0D) {
//                float s = (float) inst.getValue();
//                event.getPoseStack().pushPose();
//                event.getPoseStack().scale(s, s, s);
//            }
//        }
//
//        @SubscribeEvent
//        public static void onRenderPost(RenderLivingEvent.Post<?, ?> event) {
//            Attribute mikuSize = getMikuSizeAttr();
//            var inst = event.getEntity().getAttribute(mikuSize);
//            if (inst != null && inst.getValue() != 1.0D) {
//                event.getPoseStack().popPose();
//            }
//        }
//    }如果想玩2次放大就弄回来

    // --- 逻辑处理部分（保持原有逻辑，仅更新属性引用） ---

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
        double radius = 12.0D;
        AABB area = player.getBoundingBox().inflate(radius);
        List<LivingEntity> targets = world.getEntitiesOfClass(LivingEntity.class, area,
                e -> e != player && isFriendly(e, player));
        for (LivingEntity target : targets) {
            applyAreaBlessing(target, stack);
        }
    }

    public static void applySingleBlessing(LivingEntity target, ItemStack stack) {
        float oldMax = target.getMaxHealth();
        int upLevel = stack.getOrCreateTag().getInt("UpgradeLevel");
        int durUp = stack.getOrCreateTag().getInt("DurationUp");
        int singleLevel = 1 + upLevel;
        int duration = 400 + (durUp * 200);

        applyCommonEffects(target, duration, singleLevel, true);

        float step = 1.0F + (singleLevel * 0.1F);

        modify(target, Attributes.MAX_HEALTH, 0.5, MIKU_BUFF_ID);
        modify(target, Attributes.MOVEMENT_SPEED, 0.2 * step, MIKU_BUFF_ID);
        modify(target, Attributes.ARMOR, 0.2 * step, MIKU_BUFF_ID);
        modify(target, Attributes.ATTACK_DAMAGE, 0.25 * step, MIKU_BUFF_ID);
        modify(target, Attributes.ARMOR_TOUGHNESS, 0.25 * step, MIKU_BUFF_ID);

        // 使用 Dream Sakura 属性进行缩放
        modify(target, getMikuSizeAttr(), 0.25, MIKU_BUFF_ID);

        float addedHealth = target.getMaxHealth() - oldMax;
        if (addedHealth > 0) {
            target.setHealth(target.getHealth() + addedHealth);
        }

        target.getPersistentData().putLong("MikuBuffEnd_Single", target.level().getGameTime() + duration);
    }

    public static void applyAreaBlessing(LivingEntity target, ItemStack stack) {
        int upLevel = stack.getOrCreateTag().getInt("UpgradeLevel");
        int durUp = stack.getOrCreateTag().getInt("DurationUp");
        float oldMax = target.getMaxHealth();
        int areaLevel = 0 + upLevel;
        int duration = 600 + (durUp * 200);

        applyCommonEffects(target, duration, areaLevel, false);

        long currentTime = target.level().getGameTime();
        CompoundTag data = target.getPersistentData();
        boolean singleActive = data.contains("MikuBuffEnd_Single") && currentTime <= data.getLong("MikuBuffEnd_Single");

        if (!singleActive) {
            modify(target, Attributes.MAX_HEALTH, 0.25, MIKU_BUFF_ID);
            modify(target, Attributes.ARMOR, 0.1, MIKU_BUFF_ID);
            modify(target, Attributes.ATTACK_DAMAGE, 0.1, MIKU_BUFF_ID);

            // 使用 Dream Sakura 属性进行缩放
            modify(target, getMikuSizeAttr(), 0.10, MIKU_BUFF_ID);

            float addedHealth = target.getMaxHealth() - oldMax;
            if (addedHealth > 0) {
                target.setHealth(target.getHealth() + addedHealth);
            }
        }

        target.getPersistentData().putLong("MikuBuffEnd_Area", target.level().getGameTime() + duration);
    }

    private static void applyCommonEffects(LivingEntity target, int duration, int levelAmp, boolean isShiftRightClick) {
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, duration, levelAmp));
        target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, levelAmp));

        if (target.isInvertedHealAndHarm()) {
            addEffectIfPresent(target, "goety:necrosis", duration, levelAmp);
        } else {
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, levelAmp));
        }

        int asylumAmp = isShiftRightClick ? 1 : 0;
        addEffectIfPresent(target, "more_potion_effects:asylum", duration, asylumAmp);
        addEffectIfPresent(target, "goety:deflective", duration, Math.min(2, levelAmp));

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

            boolean hasSingle = data.contains("MikuBuffEnd_Single");
            boolean hasArea = data.contains("MikuBuffEnd_Area");
            boolean singleActive = hasSingle && currentTime <= data.getLong("MikuBuffEnd_Single");
            boolean areaActive = hasArea && currentTime <= data.getLong("MikuBuffEnd_Area");

            Attribute mikuSize = getMikuSizeAttr();

            if (hasSingle && !singleActive) {
                data.remove("MikuBuffEnd_Single");
                if (areaActive) {
                    modify(entity, Attributes.MAX_HEALTH, 0.25, MIKU_BUFF_ID);
                    modify(entity, Attributes.ARMOR, 0.1, MIKU_BUFF_ID);
                    modify(entity, Attributes.ATTACK_DAMAGE, 0.1, MIKU_BUFF_ID);
                    modify(entity, mikuSize, 0.10, MIKU_BUFF_ID);

                    removeModifier(entity, Attributes.MOVEMENT_SPEED, MIKU_BUFF_ID);
                    removeModifier(entity, Attributes.ARMOR_TOUGHNESS, MIKU_BUFF_ID);
                }
            }

            if (hasArea && !areaActive) {
                data.remove("MikuBuffEnd_Area");
            }

            if (!singleActive && !areaActive && (hasSingle || hasArea)) {
                removeModifier(entity, Attributes.MAX_HEALTH, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.MOVEMENT_SPEED, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.ARMOR, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.ATTACK_DAMAGE, MIKU_BUFF_ID);
                removeModifier(entity, Attributes.ARMOR_TOUGHNESS, MIKU_BUFF_ID);
                removeModifier(entity, mikuSize, MIKU_BUFF_ID);
            }
        }
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

    private static void modify(LivingEntity e, Attribute a, double v, UUID u) {
        var i = e.getAttribute(a);
        if (i != null) {
            i.removeModifier(u);
            i.addTransientModifier(new AttributeModifier(u, "LaMeowBuff", v, AttributeModifier.Operation.MULTIPLY_TOTAL));

            if (a == getMikuSizeAttr()) {
                e.refreshDimensions();
            }
        }
    }

    private static void removeModifier(LivingEntity e, Attribute a, UUID u) {
        var i = e.getAttribute(a);
        if (i != null) i.removeModifier(u);
    }

    private static void addEffectIfPresent(LivingEntity entity, String id, int duration, int amp) {
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
        if (effect != null) {
            entity.addEffect(new MobEffectInstance(effect, duration, amp));
        }
    }
}