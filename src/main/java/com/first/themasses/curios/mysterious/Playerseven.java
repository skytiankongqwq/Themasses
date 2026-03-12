package com.first.themasses.curios.mysterious;
//1.攻击无视无敌帧
//2.满血伤害*2，一滴血或者1%血量的时候伤害*7
//3.拿着石剑的时候wasd的移动都和疾跑速度一样（即便是空中转向)
//4.对boss标签的生物攻击伤害增加10%（随攻击次数提高，每次10%，被攻击到则重置）
//5.拿着剑类武器时面板伤害提高50%
//6.踩在原石上获得持续的生命恢复3
//7.每斩杀一个有boss标签的生物，会提升自身50%的属性，同一个生物只会增加一次)
//8.在不同时间段使用技能会获得不同的效果
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
//p7饰品
public class Playerseven extends Item implements ICurio {

    public Playerseven(Properties properties) {
        super(properties);
    }

    // --- 1. 按键触发 (在 Packet 中调用此方法) ---
    public static void triggerAbility(ServerPlayer player) {
        long currentTime = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();

        // --- CD 判定 ( 2400 ticks) ---
        if (data.contains("P7_Ability_CD")) {
            long lastUsed = data.getLong("P7_Ability_CD");
            if (currentTime - lastUsed < 2400) {
                long remainingSec = (2400 - (currentTime - lastUsed)) / 20;
                player.sendSystemMessage(Component.literal("§c技能冷却中... 剩余 " + remainingSec + " 秒"));
                return;
            }
        }

        long timeOfDay = player.level().getDayTime() % 24000;

        // 0-8000: 晨曦之力 (6:00 - 14:00)
        if (timeOfDay >= 0 && timeOfDay < 8000) {
            // 判定是否有雨 (下雨或雷雨)
            boolean isRaining = player.level().isRaining() || player.level().isThundering();
            // 有雨3分钟(3600t)，无雨1分钟(1200t)
            int duration = isRaining ? 3600 : 1200;

            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, 9)); // 10级Buff (等级从0开始算)
            player.sendSystemMessage(Component.literal("§e黎明之幕开启，破碎的躯壳在初阳中重构..." +
                    (isRaining ? "§f万物在雨中狂欢，蓬勃的生命力正破土而出！" : "§f伤口极速愈合中...")));
        }

        // 8000-16000: 正午之炎 (14:00 - 22:00)
        else if (timeOfDay >= 8000 && timeOfDay < 16000) {
            // 设置 1 分钟 (1200t) 的计时器
            data.putInt("P7_NoonFire", 1200);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 1200, 9)); // 10级抗火
            player.sendSystemMessage(Component.literal("此刃将焚尽世间诸恶！"));
        }

        // 16000-24000: 夜月之华 (22:00 - 6:00)
        else {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 3600, 9)); // 夜视3分钟
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 1200, 9)); // 隐身1分钟
            player.sendSystemMessage(Component.literal("身形隐于寂灭月影..."));
        }

        // 更新 CD 记录
        data.putLong("P7_Ability_CD", currentTime);
    }


    @Override
    public void curioTick(SlotContext slotContext) {
        // 每秒标记一次玩家正在佩戴此饰品，持续 20 ticks
        slotContext.entity().getPersistentData().putInt("PlayerSevenActive", 20);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> this);
        return new ICapabilityProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CuriosCapability.ITEM) return (LazyOptional<T>) (Object) lazyCurio;
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.themasses.player7.line1").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8B0000))));
    }

    @Override
    public ItemStack getStack() { return ItemStack.EMPTY; }

    @Override
    public boolean canEquipFromUse(SlotContext slotContext) { return true; }
}
