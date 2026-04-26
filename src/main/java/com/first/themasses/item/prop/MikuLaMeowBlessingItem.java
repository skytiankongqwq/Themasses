package com.first.themasses.item.prop;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MikuLaMeowBlessingItem extends Item {
    public MikuLaMeowBlessingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    /**
     * 处理在物品栏中，玩家拿着其他物品右键点击这把法杖的逻辑
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
        if (pAction == ClickAction.SECONDARY && !pOther.isEmpty()) {
            CompoundTag tag = pStack.getOrCreateTag();
            boolean changed = false;

            // 1. 等级强化 (红石块 +1, 红石粉 -1)，最高强化 2 次 (到达右键3级/Shift4级)
            if (pOther.is(Items.REDSTONE_BLOCK)) {
                int lvl = tag.getInt("UpgradeLevel");
                if (lvl < 2) { tag.putInt("UpgradeLevel", lvl + 1); changed = true; }
            } else if (pOther.is(Items.REDSTONE)) {
                int lvl = tag.getInt("UpgradeLevel");
                if (lvl > 0) { tag.putInt("UpgradeLevel", lvl - 1); changed = true; }
            }
            // 2. 时间强化 (萤石 +1, 萤石粉 -1)，最高强化 5 次 (+50s)
            else if (pOther.is(Items.GLOWSTONE)) {
                int dur = tag.getInt("DurationUp");
                if (dur < 5) { tag.putInt("DurationUp", dur + 1); changed = true; }
            } else if (pOther.is(Items.GLOWSTONE_DUST)) {
                int dur = tag.getInt("DurationUp");
                if (dur > 0) { tag.putInt("DurationUp", dur - 1); changed = true; }
            }
            // 3. 冷却缩减 (铁块 +1, 铁锭 -1)，最高强化 5 次 (-25s)
            else if (pOther.is(Items.IRON_BLOCK)) {
                int cd = tag.getInt("CooldownUp");
                if (cd < 5) { tag.putInt("CooldownUp", cd + 1); changed = true; }
            } else if (pOther.is(Items.IRON_INGOT)) {
                int cd = tag.getInt("CooldownUp");
                if (cd > 0) { tag.putInt("CooldownUp", cd - 1); changed = true; }
            }

            // 如果强化成功，消耗材料并播放音效
            if (changed) {
                pOther.shrink(1); // 消耗一个材料
                pPlayer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.2F);
                return true; // 拦截原生点击事件
            }
        }
        return super.overrideOtherStackedOnMe(pStack, pOther, pSlot, pAction, pPlayer, pAccess);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        String rawText = Component.translatable("tooltip.themasses.mikuprop.line1").getString();
        MutableComponent finalComponent = Component.empty();
        long time = System.currentTimeMillis() / 50;

        for (int i = 0; i < rawText.length(); i++) {
            float ratio = (float) (Math.sin((time + i * 0.6)) * 0.5 + 0.5);
            int green = (int) (255 * (1 - ratio) + 119 * ratio);
            int color = (255 << 16) | (green << 8) | 255;
            finalComponent.append(Component.literal(String.valueOf(rawText.charAt(i)))
                    .withStyle(net.minecraft.network.chat.Style.EMPTY.withColor(net.minecraft.network.chat.TextColor.fromRgb(color))));
        }
        tooltip.add(finalComponent);
    }
}
//    @Override
//    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
//        String rawText = Component.translatable("tooltip.themasses.mikuprop.line1").getString();
//        MutableComponent finalComponent = Component.empty();
//
//        // 1. 获取当前系统时间。除以 50 是为了让速度适中，数字越大变换越慢
//        long time = System.currentTimeMillis() / 200;
//
//        for (int i = 0; i < rawText.length(); i++) {
//            // 2. 利用正弦函数计算一个 0.0 到 1.0 之间的动态比例
//            // time 驱动颜色随时间滚动，i * 0.6 产生字与字之间的颜色偏差（波浪感）
//            float ratio = (float) (Math.sin((time + i * 0.6)) * 0.5 + 0.5);
//            // 3. 在白色 (255, 255, 255) 和 粉色 (255, 119, 255) 之间进行线性插值 (LERP)
//            // 红色始终是 255
//            int red = 255;
//            // 绿色从 255 变到 119
//            int green = (int) (255 * (1 - ratio) + 119 * ratio);
//            // 蓝色始终是 255
//            int blue = 255;
//            // 组合成 RGB 颜色
//            int color = (red << 16) | (green << 8) | blue;
//            // 4. 追加带颜色的字符
//            finalComponent.append(Component.literal(String.valueOf(rawText.charAt(i)))
//                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color))));
//        }
//
//        tooltip.add(finalComponent);
//    }
