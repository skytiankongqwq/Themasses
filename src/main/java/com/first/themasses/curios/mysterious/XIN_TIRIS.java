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
//M3饰品
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
        // 1. 获取饰品槽位
        var curioStackOpt = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                .findFirstCurio(player, stack -> stack.getItem() instanceof XIN_TIRIS);

        if (curioStackOpt.isEmpty()) return;

        Item curioItem = curioStackOpt.get().stack().getItem();
        // 修正：正确的类名是 ItemCooldowns
        net.minecraft.world.item.ItemCooldowns tracker = player.getCooldowns();

        // 2. 检查冷却并计算精确秒数
        if (tracker.isOnCooldown(curioItem)) {
            // 获取冷却进度 (1.0 为刚开始，0.0 为结束)
            float progress = tracker.getCooldownPercent(curioItem, 0.0F);

            // 总时间 360秒 (7200 ticks)
            int totalTicks = 7200;
            int remainingTicks = Math.round(progress * totalTicks);
            int remainingSeconds = remainingTicks / 20;

            player.displayClientMessage(Component.literal("技能冷却中... 剩余 " + remainingSeconds + " 秒")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // --- 技能逻辑开始 ---
        Level level = player.level();
        Vec3 look = player.getLookAngle();
        AABB area = player.getBoundingBox().expandTowards(look.scale(6.0)).inflate(1.5);
        List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area);

        for (LivingEntity target : targets) {
            if (target == player) continue;

            float healthDiff = Math.abs(player.getHealth() - target.getHealth());
            float baseAtk = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);


            target.hurt(player.damageSources().playerAttack(player), healthDiff);

            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1, target.getZ(), 1, 0, 0, 0, 0);
            }
        }

        // 3. 触发 360 秒冷却
        tracker.addCooldown(curioItem, 7200);
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