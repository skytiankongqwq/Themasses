package com.first.themasses.curios.mysterious;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
//月灼的饰品类
public class Lyuezhuo extends Item implements ICurio {

    public Lyuezhuo(Properties properties) {
        super(properties);
    }

    // --- 1. Capability 注入 ---
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

            public void invalidateCaps() {
                lazyCurio.invalidate();
            }
        };
    }

    // --- 2. 永久夜视逻辑：每 Tick 检查并刷新 ---
    @Override
    public void curioTick(SlotContext slotContext) {
        LivingEntity entity = slotContext.entity();

        // 确保只在服务端执行逻辑，并且目标是玩家
        if (!entity.level().isClientSide && entity instanceof Player player) {

            // 每隔 200 ticks (~10秒) 刷新一次夜视效果
            if (entity.level().getGameTime() % 10 == 0) {

                int duration = 240; // 12秒
                int amplifier = 0; // 0 级夜视

                // 关键：设置 showParticles: false 和 showIcon: false 来隐藏效果图标和粒子
                MobEffectInstance nightVisionEffect = new MobEffectInstance(
                        MobEffects.NIGHT_VISION,
                        duration,
                        amplifier,
                        false, // isAmbient
                        false, // showParticles: false
                        false  // showIcon: false
                );

                player.addEffect(nightVisionEffect);
            }
        }
    }

    // --- 3. 移除夜视效果 ---
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack) {
        LivingEntity entity = slotContext.entity();

        if (!entity.level().isClientSide && entity instanceof Player player) {
            // 在玩家取下饰品时，移除夜视效果
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    // --- 4. Tooltip 冷却时间显示 ---
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // 基础描述
        Component line1 = Component.translatable("tooltip.themasses.lyuezhuo.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA)));
        tooltip.add(line1);
    }

    // --- 5. ICurio 接口要求的其余方法 ---
    @Override
    public ItemStack getStack() { return ItemStack.EMPTY; }
    @Override
    public boolean canEquip(SlotContext slotContext) { return true; }
    @Override
    public boolean canUnequip(SlotContext slotContext) { return true; }
    // onEquip 不执行特殊逻辑
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack) { }
}