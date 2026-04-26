package com.first.themasses.curios.mysterious;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import javax.annotation.Nullable;
import java.util.List;

public class SweetToothRelicItem extends Item implements ICurioItem {
    public SweetToothRelicItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (!slotContext.entity().level().isClientSide && slotContext.entity() instanceof Player player) {
            if (player.tickCount % 200 == 0) {
                stack.getOrCreateTag().putInt("SugarCount", stack.getOrCreateTag().getInt("SugarCount") + 1);
            }
        }
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
        if (pAction == ClickAction.SECONDARY && pOther.is(Items.SUGAR)) {
            pStack.getOrCreateTag().putInt("SugarCount", pStack.getOrCreateTag().getInt("SugarCount") + pOther.getCount());
            pOther.setCount(0);
            pPlayer.playSound(SoundEvents.ITEM_PICKUP, 0.5F, 1.5F);
            return true;
        }
        return super.overrideOtherStackedOnMe(pStack, pOther, pSlot, pAction, pPlayer, pAccess);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        int count = stack.getOrCreateTag().getInt("SugarCount");
        tooltip.add(Component.literal("储存的糖: " + count).withStyle(ChatFormatting.GOLD));
    }
}