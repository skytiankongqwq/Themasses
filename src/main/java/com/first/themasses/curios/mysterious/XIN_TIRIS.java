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

// M3饰品 - 大理石修正版
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

                // --- 1. 实时记录受到过的负面效果 (每 tick 检查以防漏掉短时间效果) ---
                CompoundTag tag = stack.getOrCreateTagElement("RecordedNegativeEffects");
                for (MobEffectInstance effect : player.getActiveEffects()) {
                    if (!effect.getEffect().isBeneficial()) {
                        ResourceLocation effectId = ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect());
                        if (effectId != null) {
                            String key = effectId.toString();
                            int currentAmp = effect.getAmplifier();
                            // 如果没记录过，或者新等级更高，则更新记录
                            if (!tag.contains(key) || tag.getInt(key) < currentAmp) {
                                tag.putInt(key, currentAmp);
                            }
                        }
                    }
                }

                // 性能优化：回血逻辑每秒 (20 tick) 判定一次
                if (player.tickCount % 20 == 0) {
                    int types = tag.getAllKeys().size();
                    int totalLevel = 0;
                    for (String key : tag.getAllKeys()) {
                        // Amplifier 从 0 开始，所以真实层数是 +1
                        totalLevel += (tag.getInt(key) + 1);
                    }

                    // --- 2. 敌对怪物与记录层数回血 ---
                    // 扫描 16 格内以玩家为目标的敌对生物
                    List<Mob> haters = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0),
                            mob -> mob instanceof Enemy && mob.getTarget() == player);

                    if (types > 0 && totalLevel > 0) {
                        // 恢复公式：(敌对怪物数量 + 记录的效果总层数) * 记录的项数总和
                        float healAmount = (haters.size() + totalLevel) * types;
                        if (healAmount > 0) {
                            player.heal(healAmount);
                        }
                    }

                    // --- 4. Y轴正方向检测回血 (6点/s，大理石修正：抬头看天) ---
                    Vec3 look = player.getViewVector(1.0F);
                    if (look.y > 0.9) {
                        player.heal(6.0F);
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

    /**
     * 主动技能逻辑：由 PacketAbilityKey 调用 (-444)
     */
    public static void executeXINTirisSkill(ServerPlayer player) {
        var curioStackOpt = CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS);
        if (curioStackOpt.isEmpty()) return;

        Item curioItem = curioStackOpt.get().stack().getItem();
        net.minecraft.world.item.ItemCooldowns tracker = player.getCooldowns();

        if (tracker.isOnCooldown(curioItem)) {
            float progress = tracker.getCooldownPercent(curioItem, 0.0F);
            int remainingSeconds = Math.round(progress * 7200) / 20;
            player.displayClientMessage(Component.literal("技能冷却中... 剩余 " + remainingSeconds + " 秒")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        Level level = player.level();
        Vec3 look = player.getLookAngle();

        // --- 核心修改：调整粒子配比以实现“青色龙息” ---
        if (level instanceof ServerLevel serverLevel) {
            for (int i = 0; i < 6; i++) {
                Vec3 pPos = player.getEyePosition().add(look.scale(i));

                // 2. 发光粒子 (提供那种亮青色的闪烁感)
                serverLevel.sendParticles(ParticleTypes.GLOW, pPos.x, pPos.y - 0.2, pPos.z, 4, 0.1, 0.1, 0.1, 0.01);


            }
        }

        // 范围判定 6*3
        AABB area = player.getBoundingBox().expandTowards(look.scale(6.0)).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (target == player) continue;

            float healthDiff = Math.abs(player.getHealth() - target.getHealth());
            float finalDamage = Math.min(healthDiff, target.getMaxHealth() * 0.06F);

            if (finalDamage > 0) {
                target.hurt(player.damageSources().playerAttack(player), finalDamage);
                if (level instanceof ServerLevel serverLevel) {
                    // 命中时也改为青色系粒子
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                    serverLevel.sendParticles(ParticleTypes.GLOW, target.getX(), target.getY() + 1, target.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }

        tracker.addCooldown(curioItem, 7200);
    }

    /**
     * --- 3. 攻击伤害倍增逻辑 ---
     * 监听实体受击事件。如果攻击者是玩家且佩戴了此饰品，按照公式放大伤害。
     */
    @SubscribeEvent
    public static void onPlayerAttack(LivingHurtEvent event) {
        // 1. 增加类型判定：只处理玩家直接物理攻击，排除已经处理过的“自定义伤害”
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK)) {
            if (event.getSource().getEntity() instanceof Player player) {

                CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS).ifPresent(slotResult -> {
                    ItemStack stack = slotResult.stack();
                    CompoundTag tag = stack.getTagElement("RecordedNegativeEffects");

                    if (tag != null && !tag.isEmpty()) {
                        int types = tag.getAllKeys().size();
                        int totalLevel = 0;
                        for (String key : tag.getAllKeys()) {
                            totalLevel += (tag.getInt(key) + 1);
                        }

                        if (types > 0 && totalLevel > 0) {
                            float multiplier = (float) types * totalLevel;

                            // 2. 核心修复：计算新伤害后，使用一个不会触发重复判定的伤害来源
                            float finalAmount = event.getAmount() * multiplier;

                            // 如果 finalAmount 异常大，可以在这里加一个日志打印输出调试
                            // System.out.println("DEBUG: Multiplier=" + multiplier + " Final=" + finalAmount);

                            event.setAmount(finalAmount);
                        }
                    }
                });
            }
        }
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack).copy().withStyle(s -> s.withColor(0x00AAAA));
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.xin_tiris.line1").withStyle(ChatFormatting.AQUA));

        // 动态显示当前记录的负面效果层数
        CompoundTag tag = stack.getTagElement("RecordedNegativeEffects");
        if (tag != null) {
            int types = tag.getAllKeys().size();
            int totalLevel = 0;
            for (String key : tag.getAllKeys()) {
                totalLevel += (tag.getInt(key) + 1);
            }
            tooltip.add(Component.literal("§8已记录负面效果种类: §c" + types));
            tooltip.add(Component.literal("§8已记录负面效果总层数: §c" + totalLevel));
        } else {
            tooltip.add(Component.literal("§8[尚无负面效果记录]"));
        }

        super.appendHoverText(stack, level, tooltip, flag);
    }
}