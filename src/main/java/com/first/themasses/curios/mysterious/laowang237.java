package com.first.themasses.curios.mysterious;

import com.first.themasses.effect.ModEffects;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.UUID;
//老王的饰品类
public class laowang237 extends Item implements ICurio {

    private static final UUID LAOWANG237_HEALTH_UUID = UUID.fromString("0a643d9c-1002-4b2a-a92e-336c5332f144");
    private static final UUID LAOWANG237_REACH_UUID = UUID.fromString("b72e1c9d-1111-4b2a-a92e-336c5332f144");

    public laowang237(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> this);
        return new ICapabilityProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CuriosCapability.ITEM) {
                    return (LazyOptional<T>) (Object) lazyCurio;
                }
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid) {
        if (slotContext.identifier().equals("mysterious")) {
            HashMultimap<Attribute, AttributeModifier> attributes = HashMultimap.create();
            attributes.put(Attributes.MAX_HEALTH, new AttributeModifier(LAOWANG237_HEALTH_UUID, "laowang237_health_bonus", 10.0, AttributeModifier.Operation.ADDITION));
            attributes.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(LAOWANG237_REACH_UUID, "laowang237_reach_bonus", 5.0, AttributeModifier.Operation.ADDITION));
            return attributes;
        }
        return ICurio.super.getAttributeModifiers(slotContext, uuid);
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        LivingEntity entity = slotContext.entity();
        if (entity.level().isClientSide) return;

        // 1. 获取当前计时
        int standTicks = entity.getPersistentData().getInt("laowang_stand_ticks");

        // 2. 判定是否“正在移动”
        // 判定 A: 检查按键意图 (WASD) - 这是最准的
        boolean hasInput = (entity instanceof Player p && (p.xxa != 0 || p.zza != 0));

        // 判定 B: 检查实际位移，但给一个极小的容差（比如 0.001），忽略浮点数抖动
        double dx = Math.abs(entity.getX() - entity.xo);
        double dz = Math.abs(entity.getZ() - entity.zo);
        boolean hasMoved = (dx > 0.001D || dz > 0.001D);

        // 判定 C: 检查速度向量
        boolean hasVelocity = entity.getDeltaMovement().horizontalDistanceSqr() > 0.0001D;

        // 只要有输入、有明显位移、或有明显速度，就视为移动
        boolean isMoving = hasInput || hasMoved || hasVelocity || entity.isSprinting();

        if (!isMoving) {
            // 只有静止时才计时
            standTicks++;

            // 如果 Buff 没了（比如刚才被打断），但计时器已经由于之前的站立达到了 60，
            // 说明需要开启新一轮的 3 秒等待。
            if (!entity.hasEffect(ModEffects.THINKING_QUIETLY.get()) && standTicks >= 61) {
                standTicks = 0;
            }
        } else {
            // 只要动了，计时归零，清除 Buff
            standTicks = 0;
            if (entity.hasEffect(ModEffects.THINKING_QUIETLY.get())) {
                entity.removeEffect(ModEffects.THINKING_QUIETLY.get());
            }
        }

        // 3. 达到 3 秒 (60 ticks) 触发
        if (standTicks >= 60) {
            MobEffectInstance activeEffect = entity.getEffect(ModEffects.THINKING_QUIETLY.get());
            if (activeEffect == null || activeEffect.getDuration() <= 190) {
                entity.addEffect(new MobEffectInstance(ModEffects.THINKING_QUIETLY.get(), 200, 0, false, true));
            }
        }

        // 更新数据
        entity.getPersistentData().putInt("laowang_stand_ticks", standTicks);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.themasses.laowang237.line1").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA))));
    }

    // 修复 ItemStack.EMPTY 问题：Curios 接口要求返回当前物品堆
    @Override
    public ItemStack getStack() { return new ItemStack(this); }

    @Override
    public boolean canEquip(SlotContext slotContext) { return true; }

    @Override
    public boolean canUnequip(SlotContext slotContext) { return true; }
}