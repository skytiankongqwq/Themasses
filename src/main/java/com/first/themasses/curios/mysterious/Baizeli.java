package com.first.themasses.curios.mysterious;

import com.first.themasses.sounds.MusicManager;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
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

public class Baizeli extends Item {

    private static final ResourceLocation SKILL_SOUND = new ResourceLocation("themasses", "haqi");

    public Baizeli(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // 创建 ICurio 接口的匿名实现
        ICurio curioProxy = new ICurio() {
            @Override
            public void curioTick(SlotContext slotContext) {
                // 关键：在这里将单参数转交给外部类的双参数自定义方法
                Baizeli.this.curioTick(slotContext, stack);
            }

            @Override
            public ItemStack getStack() {
                return stack;
            }

            @Override
            public boolean canEquip(SlotContext slotContext) {
                return true;
            }

            @Override
            public boolean canUnequip(SlotContext slotContext) {
                return true;
            }
        };

        return new ICapabilityProvider() {
            private final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> curioProxy);

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CuriosCapability.ITEM) {
                    return lazyCurio.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    /**
     * 自定义 Tick 逻辑（不带 @Override）
     * 这样就可以自由使用 slotContext 和 stack 参数
     */
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();

        // 仅在服务端处理逻辑
        if (entity instanceof Player player && !player.level().isClientSide) {
            CompoundTag nbt = stack.getOrCreateTag();

            // 1. 处理技能主逻辑
            if (nbt.getBoolean("SkillActive")) {
                this.executeBaizeliSkillLogic(player, nbt);
            }
            // 2. 处理冷却逻辑 (只有技能不活跃时才减 CD)
            else if (nbt.getInt("SkillCD") > 0) {
                nbt.putInt("SkillCD", nbt.getInt("SkillCD") - 1);
            }
        }
    }

    /**
     * 核心技能逻辑处理器
     */
    private void executeBaizeliSkillLogic(Player player, CompoundTag nbt) {
        int timer = nbt.getInt("SkillTimer");

        // A. 音频播放点 (0s, 2s, 4s)
        if (timer == 0 || timer == 40 || timer == 80) {
            // 播放音频
            net.minecraft.sounds.SoundEvent sound = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.getValue(SKILL_SOUND);
            if (sound != null) {
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        sound, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            // 亮起字幕：青色加粗
            player.displayClientMessage(Component.literal("§b§l哈！"), true);
        }

        // --- 2. 熄灭字幕 ---
        // 在亮起后的第 10 刻（约0.5秒）熄灭，这样就不会有重复闪烁了
        if (timer == 10 || timer == 50 || timer == 90) {
            player.displayClientMessage(Component.literal(""), true);
        }

        // B. 无AI 效果判定 (同步音频后的 1秒)
        if (this.isSkillEffectWindow(timer)) {
            this.applyFreezeToNearbyMobs(player);
        }

        // C. 计时累加
        timer++;
        nbt.putInt("SkillTimer", timer);

        // D. 序列结束判定：在 100 ticks 后结束，进入 20 ticks 的 CD
        if (timer >= 100) {
            nbt.putBoolean("SkillActive", false);
            nbt.putInt("SkillTimer", 0);
            nbt.putInt("SkillCD", 140);
        }
    }

    /**
     * 判定是否处于 1s 的无 AI 生效窗口
     */
    private boolean isSkillEffectWindow(int timer) {
        return (timer >= 0 && timer < 20) || (timer >= 40 && timer < 60) || (timer >= 80 && timer < 100);
    }

    /**
     * 具体的 7x7 范围冻结执行
     */
    private void applyFreezeToNearbyMobs(Player player) {
        List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(7));
        for (Mob mob : nearbyMobs) {
            mob.setNoAi(true);
            if (!mob.getTags().contains("BaizeliFrozen")) {
                mob.addTag("BaizeliFrozen");
            }
            // 配合 BaizeliEvents 使用
            mob.getPersistentData().putInt("BaizeliFreezeTicks", 5);
        }
    }

    /**
     * 按键触发接口
     */
    public void startSkill(ItemStack stack, Player player) {
        CompoundTag nbt = stack.getOrCreateTag();

        // 1. 如果技能正在运行，直接拦截（不提示，防止刷屏）
        if (nbt.getBoolean("SkillActive")) {
            return;
        }

        // 2. 如果处于冷却时间内，提示剩余时间
        int currentCD = nbt.getInt("SkillCD");
        if (currentCD > 0) {
            // 计算秒数 (1秒 = 20 ticks)
            float remainingSeconds = currentCD / 20.0f;
            player.displayClientMessage(
                    Component.literal("§c冷却中... " + String.format("%.1f", remainingSeconds) + "s"),
                    true
            );
            return;
        }
        // 3. 正常启动技能
        nbt.putBoolean("SkillActive", true);
        nbt.putInt("SkillTimer", 0);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.themasses.Baizeli.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA))));
    }
}