package com.first.themasses.curios.mysterious;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;

public class YanWenZi extends Item {

    public YanWenZi(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    // --- 1. 核心逻辑：触发免死与击退 ---
    public static void triggerDeathDefy(ServerPlayer player, ItemStack stack) {
        player.getCooldowns().addCooldown(stack.getItem(), 800); // 40秒冷却

        player.setHealth(2.0F);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 4));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 300, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 1));

        ServerLevel level = player.serverLevel();
        AABB area = player.getBoundingBox().inflate(5.0D, 2.0D, 5.0D);
        List<LivingEntity> enemies = level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player);

        for (LivingEntity enemy : enemies) {
            Vec3 dir = enemy.position().subtract(player.position()).normalize().scale(1.2);
            enemy.setDeltaMovement(new Vec3(dir.x, 0.8D, dir.z));
            enemy.hurtMarked = true;
        }

        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, player.getX(), player.getY() + 1, player.getZ(), 1, 0, 0, 0, 0);
        player.displayClientMessage(Component.literal("§6★ 守护神迹：致命伤已抵挡！").withStyle(ChatFormatting.BOLD), true);
    }

    // --- 2. 接口实现：Capability 注入 ---
    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<ICurio> curio = LazyOptional.of(() -> new ICurio() {
                @Override
                public ItemStack getStack() {
                    return stack;
                }

                @Override
                public void curioTick(SlotContext slotContext) {
                    LivingEntity entity = slotContext.entity();
                    if (!entity.level().isClientSide && entity.tickCount % 20 == 0) {
                        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2, false, false, true));
                    }
                }

                // 如果报错提示缺少方法，通常是因为没实现 getStack 或其他 Curio 默认方法
                // 在匿名内部类里实现 ICurio 接口是最稳妥的
            });

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return CuriosCapability.ITEM.orEmpty(cap, curio);
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.yanwenzi.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA))));
        tooltip.add(Component.empty());
    }
}