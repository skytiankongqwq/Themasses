package com.first.themasses.item.food;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 灵液瓶物品。
 * ⚠️ 注意：本物品的贴图和描述源于 [神秘遗物] 的 [ichor_bottle]
 * 本类功能已修改为恶搞用途：饮用即死 (255级瞬间伤害)。
 */
public class SpiritLiquidVialItem extends Item {

    public SpiritLiquidVialItem(Properties pProperties) {
        super(pProperties);
    }

    // 1. 致命伤害和瞬间伤害效果
    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {

        // 关键：玩家喝完药水后，手上会留下玻璃瓶
        ItemStack itemstack = super.finishUsingItem(pStack, pLevel, pLivingEntity);

        // 确保只在服务器端对真正的玩家执行操作
        if (pLivingEntity instanceof Player player && !pLevel.isClientSide) {

            // 瞬间伤害 (Instant Damage/Harm) 效果
            // 级别 255 对应 MobEffectInstance 中的 amplifier 254
            player.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 254, false, false));

            // 强制玩家死亡
            player.kill();

        }

        // 返回空玻璃瓶
        if (pStack.isEmpty()) {
            return new ItemStack(Items.GLASS_BOTTLE);
        } else {
            // 如果玩家不是在创造模式，并且没有留下空瓶子，则在背包中添加空瓶子
            if (pLivingEntity instanceof Player player && !player.getAbilities().instabuild) {
                if (itemstack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
            }
            return itemstack; // 正常返回物品，如果玩家在创造模式
        }
    }

    // 2. 添加自定义文本 (Tooltip)
    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {

        // --- 1. 默认可见的第一行文本 ---
        pTooltip.add(Component.translatable("tooltip.themasses.spirit_liquid_vial.default_line1").withStyle(ChatFormatting.DARK_PURPLE));

        // --- 2. 按住 Shift 才会显示的详细文本 ---
        if (Screen.hasShiftDown()) {

            // 详细信息（第 1 行）
            pTooltip.add(Component.translatable("tooltip.themasses.spirit_liquid_vial.shift_line1").withStyle(ChatFormatting.DARK_PURPLE).withStyle(ChatFormatting.ITALIC));

            // 详细信息（第 2 行）：使 +1 变为黄色
            Component jiayi = Component.literal("+1").withStyle(ChatFormatting.YELLOW);
            pTooltip.add(Component.translatable("tooltip.themasses.spirit_liquid_vial.shift_line2", jiayi)
                    .withStyle(ChatFormatting.DARK_PURPLE).withStyle(ChatFormatting.ITALIC));

        } else {
            // 3. 提示玩家按住 Shift 键查看详细信息（Shift 键为黄色）

            // 创建黄色的 "Shift" 文本组件
            Component shiftKey = Component.literal("Shift").withStyle(ChatFormatting.YELLOW);

            // 使用参数化翻译键，并将黄色的 shiftKey 作为参数传入
            pTooltip.add(Component.translatable("tooltip.themasses.shift_prompt", shiftKey)
                    .withStyle(ChatFormatting.DARK_PURPLE) // 设置整体颜色为深紫色
                    .withStyle(ChatFormatting.ITALIC)
            );
        }
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        // 返回 true，使物品自带附魔光效（类似于附魔物品或魔法金苹果）
        return true;
    }
}