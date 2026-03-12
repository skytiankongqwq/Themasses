package com.first.themasses.curios.mysterious;

import com.first.themasses.effect.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.Collection;
import java.util.List;

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

                // 性能优化：所有回血逻辑每秒判定一次
                if (player.tickCount % 20 == 0) {

                    // --- 1. 获取药水状态 ---
                    Collection<MobEffectInstance> effects = player.getActiveEffects();
                    int types = effects.size();
                    int totalLevel = 0;
                    for (MobEffectInstance effect : effects) {
                        totalLevel += (effect.getAmplifier() + 1);
                    }

                    // --- 2. 药水基础回血 (种类 * 总层数) ---
                    if (types > 0) {
                        player.heal((float) types * totalLevel);
                    }

                    // --- 3. 仇恨回血 (仇恨数 * 药水总层数 * 药水种类) ---
                    // 扫描 16 格内以玩家为目标的敌对生物
                    List<Mob> haters = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0),
                            mob -> mob instanceof Enemy && mob.getTarget() == player);
                    if (!haters.isEmpty() && types > 0) {
                        player.heal((float) haters.size() * totalLevel * types);
                    }

                    // --- 4. Z轴正方向检测回血 (6点/s) ---
                    Vec3 look = player.getViewVector(1.0F);
                    if (look.z > 0.9) {
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
        Level level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();

        // 定义前方 6 格的 AABB 范围
        AABB area = player.getBoundingBox().expandTowards(look.scale(6.0)).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (target == player) continue;

            // 1. 计算体力差 (当前血量差值)
            float healthDiff = Math.abs(player.getHealth() - target.getHealth());

            // 2. 获取玩家基础攻击力
            float baseAtk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);

            // 3. 判定：如果血量差 < 基础攻击力，获得 60s 100% 穿透 (需自定义效果)
            if (healthDiff < baseAtk) {
                // 这里的 ModEffects.PENETRATION 需要你已经在效果类中注册
                // player.addEffect(new MobEffectInstance(ModEffects.PENETRATION.get(), 1200, 0));
            }

            // 4. 造成伤害 (来源设为玩家攻击)
            target.hurt(player.damageSources().playerAttack(player), healthDiff);

            // 5. 视觉反馈
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
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
        super.appendHoverText(stack, level, tooltip, flag);
    }
}