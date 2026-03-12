package com.first.themasses.item.prop;

//生物会对你保持友好，并会逐渐适应伤害直到免疫该伤害，附近有生物时优先消耗距离你最近的生物的生命以免疫受伤
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DreamSakuraProp extends Item {
    public DreamSakuraProp(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 获取持久化数据
            CompoundTag forgeData = player.getPersistentData();
            if (!forgeData.contains(Player.PERSISTED_NBT_TAG)) {
                forgeData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
            }
            CompoundTag persisted = forgeData.getCompound(Player.PERSISTED_NBT_TAG);
            // 每一个物品只能右键一次，第二次无效
            if (persisted.getBoolean("DreamCherryBlessed")) {
                player.displayClientMessage(Component.literal("§d[梦樱的祝福] §c你已承载过这份眷顾，无法再次触发。"), true);
                return InteractionResultHolder.fail(stack);
            }
            // 激活永久效果
            persisted.putBoolean("DreamCherryBlessed", true);
            player.displayClientMessage(Component.literal("§d[梦樱的祝福] §f樱花落尽。"), false);
            // 消耗物品
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.DreamSakura.line1").withStyle(Style.EMPTY.withColor(0x9933FF)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}