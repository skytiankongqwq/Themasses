package com.first.themasses.item.prop;

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
import java.util.Map;

public class MikuLaMeowBlessingItem extends Item {

    // 定义强化规则：材料 -> (NBT键, 变化值, 最大上限)
    private static final Map<Item, UpgradeRule> UPGRADE_RULES = Map.ofEntries(
            Map.entry(Items.REDSTONE_BLOCK,  new UpgradeRule("UpgradeLevel", 1, 2)),
            Map.entry(Items.REDSTONE,        new UpgradeRule("UpgradeLevel", -1, 2)),
            Map.entry(Items.GLOWSTONE,       new UpgradeRule("DurationUp", 1, 5)),
            Map.entry(Items.GLOWSTONE_DUST,  new UpgradeRule("DurationUp", -1, 5)),
            Map.entry(Items.IRON_BLOCK,      new UpgradeRule("CooldownUp", 1, 5)),
            Map.entry(Items.IRON_INGOT,      new UpgradeRule("CooldownUp", -1, 5)),
            Map.entry(Items.GOLD_BLOCK,      new UpgradeRule("RangeUpgrade", 1, 3)),
            Map.entry(Items.GOLD_INGOT,      new UpgradeRule("RangeUpgrade", -1, 3))
    );

    public MikuLaMeowBlessingItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
        if (pAction == ClickAction.SECONDARY && !pOther.isEmpty()) {
            // 直接从映射表中获取规则
            UpgradeRule rule = UPGRADE_RULES.get(pOther.getItem());

            if (rule != null) {
                CompoundTag tag = pStack.getOrCreateTag();
                if (applyUpgrade(tag, rule)) {
                    pOther.shrink(1);
                    pPlayer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6F, 1.2F);
                    return true;
                }
            }
        }
        return super.overrideOtherStackedOnMe(pStack, pOther, pSlot, pAction, pPlayer, pAccess);
    }

    /**
     * 核心逻辑：应用强化并返回是否成功
     */
    private boolean applyUpgrade(CompoundTag tag, UpgradeRule rule) {
        int current = tag.getInt(rule.nbtKey);
        int next = current + rule.amount;

        if (next >= 0 && next <= rule.maxLimit && next != current) {
            tag.putInt(rule.nbtKey, next);
            return true;
        }
        return false;
    }

    // 内部数据类
    private record UpgradeRule(String nbtKey, int amount, int maxLimit) {}

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