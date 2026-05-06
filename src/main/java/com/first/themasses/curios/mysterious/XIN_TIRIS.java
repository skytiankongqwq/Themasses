package com.first.themasses.curios.mysterious;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.UUID;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class XIN_TIRIS extends Item {

    public XIN_TIRIS(Properties properties) {
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
                if (entity.level().isClientSide || !(entity instanceof Player player)) return;

                CompoundTag tag = stack.getOrCreateTagElement("RecordedNegativeEffects");
                for (MobEffectInstance effect : player.getActiveEffects()) {
                    if (!effect.getEffect().isBeneficial()) {
                        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                        if (effectId != null) {
                            String key = effectId.toString();
                            int currentAmp = effect.getAmplifier();
                            if (!tag.contains(key) || tag.getInt(key) < currentAmp) {
                                tag.putInt(key, currentAmp);
                            }
                        }
                    }
                }

                if (player.tickCount % 20 == 0) {
                    int types = tag.getAllKeys().size();
                    int totalLevel = 0;
                    for (String key : tag.getAllKeys()) {
                        totalLevel += (tag.getInt(key) + 1);
                    }
                    if (types > 0 && totalLevel > 0) {
                        float healPercent = 0.001F * types;
                        float healFlat = 0.1F * totalLevel;
                        float maxHealth = player.getMaxHealth();
                        float healAmount = maxHealth * healPercent + healFlat;
                        if (healAmount > 0) player.heal(healAmount);
                    }

                    Vec3 look = player.getViewVector(1.0F);
                    if (look.y > 0.9) player.heal(6.0F);

                    AttributeInstance atkInst = player.getAttribute(Attributes.ATTACK_DAMAGE);
                    if (atkInst != null) {
                        UUID uuid = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
                        AttributeModifier existing = atkInst.getModifier(uuid);
                        if (existing != null) atkInst.removeModifier(uuid);
                        if (types > 0) {
                            atkInst.addPermanentModifier(new AttributeModifier(
                                    uuid,
                                    "xin_tiris_attack_bonus",
                                    types,
                                    AttributeModifier.Operation.ADDITION
                            ));
                        }
                    }
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

    public static void executeXINTirisSkill(ServerPlayer player) {
        var curioStackOpt = CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS);
        if (curioStackOpt.isEmpty()) return;

        Item curioItem = curioStackOpt.get().stack().getItem();
        if (player.getCooldowns().isOnCooldown(curioItem)) return;

        Level level = player.level();
        Vec3 look = player.getLookAngle();

        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 6; i++) {
                Vec3 pPos = player.getEyePosition().add(look.scale(i));
                serverLevel.sendParticles(ParticleTypes.GLOW, pPos.x, pPos.y - 0.2, pPos.z, 4, 0.1, 0.1, 0.1, 0.01);
            }
        }

        AABB area = player.getBoundingBox().expandTowards(look.scale(6.0)).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (target == player) continue;
            float finalDamage = Math.abs(player.getHealth() - target.getHealth());
            target.hurt(player.damageSources().playerAttack(player), finalDamage);
        }

        player.getCooldowns().addCooldown(curioItem, 7200);
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack).copy().withStyle(s -> s.withColor(0x00AAAA));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.xin_tiris.line1").withStyle(ChatFormatting.AQUA));
        CompoundTag tag = stack.getTagElement("RecordedNegativeEffects");
        if (tag != null) {
            int types = tag.getAllKeys().size();
            int totalLevel = 0;
            for (String key : tag.getAllKeys()) {
                totalLevel += (tag.getInt(key) + 1);
            }
            tooltip.add(Component.literal("§8已记录负面效果种类: §c" + types));
            tooltip.add(Component.literal("§8已记录负面效果总层数: §c" + totalLevel));
            tooltip.add(Component.literal("§8当前倍率: §c" + totalLevel*types));
        } else {
            tooltip.add(Component.literal("§8[尚无负面效果记录]"));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}