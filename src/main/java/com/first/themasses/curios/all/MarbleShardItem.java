package com.first.themasses.curios.all;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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

public class MarbleShardItem extends Item {

    public MarbleShardItem(Properties properties) {
        super(properties);
    }



    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        ICurio curio = new ICurio() {
            @Override
            public ItemStack getStack() {
                return stack;
            }

            @Override
            public void curioTick(SlotContext slotContext) {
                LivingEntity entity = slotContext.entity();
                Level level = entity.level();
                if (level.isClientSide && entity.tickCount % 3 == 0) {
                    level.addParticle(ParticleTypes.DRIPPING_WATER,
                            entity.getX() + (level.random.nextDouble() - 0.5D) * entity.getBbWidth(),
                            entity.getY() + level.random.nextDouble() * entity.getBbHeight(),
                            entity.getZ() + (level.random.nextDouble() - 0.5D) * entity.getBbWidth(),
                            0.0D, 0.0D, 0.0D);
                }
            }
        };

        return new ICapabilityProvider() {
            private final LazyOptional<ICurio> curioOpt = LazyOptional.of(() -> curio);
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return CuriosCapability.ITEM.orEmpty(cap, curioOpt);
            }
        };
    }

    /**
     * 主动技能逻辑：由 PacketAbilityKey 调用 (-5152)
     */
    public static void executeMarbleShardSkill(ServerPlayer player) {
        net.minecraft.server.level.ServerLevel level = player.serverLevel();
        net.minecraft.world.phys.AABB area = player.getBoundingBox().inflate(3.0D);
        java.util.List<net.minecraft.world.entity.animal.Pig> pigs = level.getEntitiesOfClass(net.minecraft.world.entity.animal.Pig.class, area);

        int affectedCount = 0;
        for (net.minecraft.world.entity.animal.Pig pig : pigs) {
            if (pig.getAge() == 0 && !pig.isInLove()) {
                pig.setInLove(player);
                affectedCount++;
                level.sendParticles(ParticleTypes.HEART,
                        pig.getX(), pig.getY() + pig.getBbHeight() + 0.2, pig.getZ(),
                        5, 0.2, 0.2, 0.2, 0.05);
            }
        }

        if (affectedCount > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.5F);
            player.displayClientMessage(Component.literal("§d大理石碎片的共鸣吸引了附近的猪... (" + affectedCount + " 只)"), true);
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.VILLAGER_NO,
                    net.minecraft.sounds.SoundSource.PLAYERS, 0.5F, 1.0F);
            player.displayClientMessage(Component.literal("§7附近没有可以接受共鸣的猪"), true);
        }
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.marble_shard.line1").withStyle(net.minecraft.ChatFormatting.GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}