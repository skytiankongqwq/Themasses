package com.first.themasses.curios.mysterious;
//1.周围8*8范围内狐狸获得不死图腾的效果，对狐狸所有攻击伤害10倍返还
//2.狐狸会主动跟随，在甜浆果丛中移动时不会受到伤害和减速作用，处在细雪上时也不会陷入其中,(但是按住shift则无法移动，其实是bug但我懒得修qwq)
//3.持续获得速度2和幸运3和跳跃3
//4.每在樱花树林鉴证一个新生物的死亡或诞生，会获得一次复活甲或共死（每个生物可以各自触发诞生和死亡一次，诞生指的是繁衍诞生，复活甲指的是死亡时血量回到100%，共死指的是下一次收到的伤害变成kill，这个死亡不会浪费复活甲的次数）死亡优先级大于复活甲
//5.使用r键可以让周围9*9范围内所有生物失去索敌ai，20s，cd180s
//6.对鸡、兔子、鳕鱼、鲑鱼、热带鱼和陆地上的幼年海龟的攻击伤害变为原来的10倍，对狼和北极熊伤害削减50%，且这两个生物会主动攻击玩家
//7.身上的绿宝石每1分钟有20%翻倍，绿宝石块则是10%
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;

public class XiaoArin extends Item implements ICurio {

    public XiaoArin(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void curioTick(SlotContext slotContext) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.level();

        if (entity instanceof Player player && !level.isClientSide) {
            // 每秒执行一次 (20 ticks)
            if (player.tickCount % 20 == 0) {
                // 3. 持续获得速度2、幸运3、跳跃3
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 210, 1, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.LUCK, 210, 2, true, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 210, 2, true, false));

                // 2. 狐狸主动跟随玩家 (8*8范围内)
                AABB area = player.getBoundingBox().inflate(8.0D);
                level.getEntitiesOfClass(Fox.class, area).forEach(fox -> {
                    if (fox.getTarget() != player) {
                        fox.setTarget(player);
                    }
                });
            }
            // 7. 每1分钟检测绿宝石翻倍
            if (player.tickCount % 1200 == 0) {
                handleEmeraldEconomy(player);
            }
        }
        //技能cd显示
        if (!entity.level().isClientSide && entity instanceof Player player) {
            CompoundTag data = player.getPersistentData();
            int displayTicks = data.getInt("ArinShowCDTicks");

            if (displayTicks > 0) {
                long currentTime = player.level().getGameTime();
                long lastUseTime = data.getLong("ArinTimeStopCD");
                long remainingSeconds = (3600 - (currentTime - lastUseTime)) / 20;

                // 只要计时器还没归零，就一直刷 Actionbar
                player.displayClientMessage(
                        Component.literal("§b[神谕·静滞] §c冷却中: §e" + Math.max(0, remainingSeconds) + "s"),
                        true
                );

                // 递减显示计时器
                data.putInt("ArinShowCDTicks", displayTicks - 1);
            }
        }
    }

    // 处理绿宝石翻倍逻辑
    private void handleEmeraldEconomy(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack invStack = player.getInventory().getItem(i);
            if (invStack.isEmpty()) continue;

            if (invStack.is(Items.EMERALD) && player.getRandom().nextFloat() < 0.20f) {
                invStack.setCount(Math.min(64, invStack.getCount() * 2));
            } else if (invStack.is(Items.EMERALD_BLOCK) && player.getRandom().nextFloat() < 0.10f) {
                invStack.setCount(Math.min(64, invStack.getCount() * 2));
            }
        }
    }

    // 5. 主动技能：R键触发静止效果 (由Packet调用)
    public static void triggerTimeStop(Player player) {
        long currentTime = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();

        // 1. 冷却检查
        long elapsed = currentTime - data.getLong("ArinTimeStopCD");
        if (elapsed < 3600) {
            // 如果在 CD 内按了技能，设置显示计时器为 40 tick (2秒)
            data.putInt("ArinShowCDTicks", 40);
            return;
        }
        // 2. 9*9生物停止
        AABB area = player.getBoundingBox().inflate(9.0D);
        player.level().getEntitiesOfClass(LivingEntity.class, area).forEach(e -> {
            if (e != player) {
                e.getPersistentData().putInt("ArinFrozenTicks", 400);
            }
        });
        // 3. 更新 CD 状态
        data.putLong("ArinTimeStopCD", currentTime);
        // 成功时把之前的 CD 提示关掉
        data.putInt("ArinShowCDTicks", 0);

        player.sendSystemMessage(Component.literal("§b[神谕·静滞] §f九阶之内的万物皆已停步。"));
        player.displayClientMessage(Component.literal("§b§l时空已凝滞"), true);
    }


    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> this);
        return new ICapabilityProvider() {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return CuriosCapability.ITEM.orEmpty(cap, lazyCurio);
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.themasses.XiaoArin.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA))));
    }

    @Override
    public ItemStack getStack() { return ItemStack.EMPTY; }
    @Override
    public boolean canEquip(SlotContext slotContext) { return true; }
    @Override
    public boolean canUnequip(SlotContext slotContext) { return true; }
}