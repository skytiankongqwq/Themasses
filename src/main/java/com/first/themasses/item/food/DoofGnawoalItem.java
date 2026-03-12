package com.first.themasses.item.food;

import com.first.themasses.effect.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DoofGnawoalItem extends Item {

    public DoofGnawoalItem() {
        super(new Item.Properties().food(
                new FoodProperties.Builder()
                        .nutrition(2)
                        .saturationMod(0.2f)
                        .alwaysEat()
                        .effect(() -> new MobEffectInstance(ModEffects.PARADOX_VIRUS.get(), 600, 0), 1.0F)
                        .build()
        ));
    }

    /**
     * 设置物品名称样式（淡蓝色）
     */
    @Override
    public @NotNull Component getName(@NotNull ItemStack pStack) {
        return Component.translatable(this.getDescriptionId(pStack))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF)));
    }

    /**
     * 添加悬浮文本（Lore）
     */
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        // 添加描述文本，使用灰色和斜体
        tooltip.add(Component.translatable("tooltip.themasses.doof_gnawoal.line1")
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}