package com.first.themasses.curios.mysterious;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
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
import net.minecraft.world.level.storage.loot.LootContext;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
//黑砂糖的饰品类
public class heishatang extends Item implements ICurioItem {

    public heishatang(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public int getFortuneLevel(SlotContext slotContext, LootContext lootContext, ItemStack stack) {
        if (stack.hasTag()) {
            int diamondPower = stack.getTag().getInt("PowerDiamond");
            return diamondPower / 32;
        }
        return 0;
    }

    @Override
    public boolean canEquip(SlotContext slotContext, ItemStack stack) {
        return top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                .findFirstCurio(slotContext.entity(), stack.getItem()).isEmpty();
    }

    @Override
    public ICurio.DropRule getDropRule(SlotContext slotContext, net.minecraft.world.damagesource.DamageSource source, int lootingLevel, boolean recentlyHit, ItemStack stack) {
        return ICurio.DropRule.ALWAYS_KEEP;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack incoming, Slot slot, ClickAction action, Player player, SlotAccess accessor) {
        if (action == ClickAction.SECONDARY && !incoming.isEmpty()) {
            CompoundTag nbt = stack.getOrCreateTag();
            boolean success = false;
            if (incoming.is(Items.EMERALD)) success = addPower(nbt, "EmeraldEquivalent", player, SoundEvents.EXPERIENCE_ORB_PICKUP, incoming, false);
            else if (incoming.is(Items.EMERALD_BLOCK)) success = addPower(nbt, "EmeraldEquivalent", player, SoundEvents.EXPERIENCE_ORB_PICKUP, incoming, true);
            else if (incoming.is(Items.COAL)) success = addPowerLimited(nbt, "PowerCoal", 100, player, SoundEvents.STONE_BREAK, incoming, false);
            else if (incoming.is(Items.COAL_BLOCK)) success = addPowerLimited(nbt, "PowerCoal", 100, player, SoundEvents.STONE_BREAK, incoming, true);
            else if (incoming.is(Items.IRON_INGOT)) success = addPower(nbt, "PowerIron", player, SoundEvents.ARMOR_EQUIP_IRON, incoming, false);
            else if (incoming.is(Items.IRON_BLOCK)) success = addPower(nbt, "PowerIron", player, SoundEvents.ARMOR_EQUIP_IRON, incoming, true);
            else if (incoming.is(Items.GOLD_INGOT)) success = addPower(nbt, "PowerGold", player, SoundEvents.ARMOR_EQUIP_GOLD, incoming, false);
            else if (incoming.is(Items.GOLD_BLOCK)) success = addPower(nbt, "PowerGold", player, SoundEvents.ARMOR_EQUIP_GOLD, incoming, true);
            else if (incoming.is(Items.LAPIS_LAZULI)) success = addPowerLimited(nbt, "PowerLapis", 16000, player, SoundEvents.ENCHANTMENT_TABLE_USE, incoming, false);
            else if (incoming.is(Items.LAPIS_BLOCK)) success = addPowerLimited(nbt, "PowerLapis", 16000, player, SoundEvents.ENCHANTMENT_TABLE_USE, incoming, true);
            else if (incoming.is(Items.REDSTONE)) success = addPowerLimited(nbt, "PowerRedstone", 500, player, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, incoming, false);
            else if (incoming.is(Items.REDSTONE_BLOCK)) success = addPowerLimited(nbt, "PowerRedstone", 500, player, SoundEvents.STONE_PRESSURE_PLATE_CLICK_ON, incoming, true);
            else if (incoming.is(Items.DIAMOND)) success = addPowerLimited(nbt, "PowerDiamond", 3200, player, SoundEvents.ARMOR_EQUIP_DIAMOND, incoming, false);
            else if (incoming.is(Items.DIAMOND_BLOCK)) success = addPowerLimited(nbt, "PowerDiamond", 3200, player, SoundEvents.ARMOR_EQUIP_DIAMOND, incoming, true);
            else if (incoming.is(Items.NETHERITE_INGOT)) success = addPowerLimited(nbt, "PowerNetherite", 9, player, SoundEvents.ARMOR_EQUIP_NETHERITE, incoming, false);
            else if (incoming.is(Items.NETHERITE_BLOCK)) success = addPowerLimited(nbt, "PowerNetherite", 9, player, SoundEvents.ARMOR_EQUIP_NETHERITE, incoming, true);
            if (success) return true;
        }
        return super.overrideOtherStackedOnMe(stack, incoming, slot, action, player, accessor);
    }

    private boolean addPower(CompoundTag nbt, String key, Player p, net.minecraft.sounds.SoundEvent sound, ItemStack incoming, boolean isBlock) {
        int amount = incoming.getCount() * (isBlock ? 9 : 1);
        nbt.putInt(key, nbt.getInt(key) + amount);
        incoming.setCount(0);
        p.playSound(sound, 0.5f, 1.2f);
        return true;
    }

    private boolean addPowerLimited(CompoundTag nbt, String key, int limit, Player p, net.minecraft.sounds.SoundEvent sound, ItemStack incoming, boolean isBlock) {
        int current = nbt.getInt(key);
        if (current >= limit) return false;
        int powerPer = isBlock ? 9 : 1;
        int needed = limit - current;
        int actual = Math.min(incoming.getCount(), (int)Math.ceil((double)needed / powerPer));
        nbt.putInt(key, Math.min(limit, current + (actual * powerPer)));
        incoming.shrink(actual);
        p.playSound(sound, 0.5f, 1.0f);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = stack.getTag();

        // 核心修改逻辑：只有 NBT 不为空 且 按住 Shift 时，才显示详情页
        if (nbt != null && Screen.hasShiftDown()) {

            // 当前能力模式
            int mode = nbt.getInt("UseMode");
            String modeName = (mode == 1 ? "【绿宝石块】" : "【绿宝石】");
            tooltip.add(Component.literal("当前能力模式: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(modeName).withStyle(mode == 1 ? ChatFormatting.AQUA : ChatFormatting.GOLD)));

            // 能量
            tooltip.add(Component.literal("储存能量: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(String.valueOf(nbt.getInt("EmeraldEquivalent"))).withStyle(ChatFormatting.GREEN)));

            // Boss 进度
            int bossKills = nbt.getInt("BossKills");
            tooltip.add(Component.literal("讨伐 Boss 数: ").withStyle(ChatFormatting.DARK_RED)
                    .append(Component.literal(String.valueOf(bossKills)).withStyle(ChatFormatting.WHITE)));

            tooltip.add(Component.literal("--- 饰品加成 ---").withStyle(ChatFormatting.GOLD));

            // 以下为各项加成显示
            int coal = nbt.getInt("PowerCoal");
            if (coal > 0) addLevelTooltip(tooltip, "煤炭", coal, 100, 100, "自动冶炼", TextColor.fromRgb(0x333333));

            int iron = nbt.getInt("PowerIron");
            if (iron > 0) tooltip.add(Component.literal("● 铁: ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xDDDDDD)))
                    .append(Component.literal("每秒修复: +" + (iron / 10)).withStyle(ChatFormatting.WHITE)));

            int gold = nbt.getInt("PowerGold");
            if (gold > 0) tooltip.add(Component.literal("● 金: ").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFD700)))
                    .append(Component.literal("幸运: +" + String.format("%.2f", gold * 0.02)).withStyle(ChatFormatting.WHITE)));

            int lapis = nbt.getInt("PowerLapis");
            if (lapis > 0) addLevelTooltip(tooltip, "青金石", lapis, 160, 16000, "效率等级: +" + (lapis / 160), TextColor.fromRgb(0x4169E1));

            int redstone = nbt.getInt("PowerRedstone");
            if (redstone > 0) addLevelTooltip(tooltip, "红石", redstone, 500, 500, "火焰附加攻击", TextColor.fromRgb(0xFF0000));

            int diamond = nbt.getInt("PowerDiamond");
            if (diamond > 0) addLevelTooltip(tooltip, "钻石", diamond, 32, 3200, "时运等级: +" + (diamond / 32), TextColor.fromRgb(0x00FFFF));

            int netherite = nbt.getInt("PowerNetherite");
            if (netherite > 0) addLevelTooltip(tooltip, "下界合金", netherite, 9, 9, "抗火效果", TextColor.fromRgb(0x4D494D));

        } else {
            // 如果没加过能量 或者 没按 Shift，统一显示 line1
            tooltip.add(Component.translatable("tooltip.themasses.heishatang.line1").withStyle(ChatFormatting.DARK_PURPLE));
        }
    }

    private void addLevelTooltip(List<Component> tooltip, String name, int total, int perLevel, int max, String activeDesc, TextColor color) {
        int currentLevel = total / perLevel;
        int progressInLevel = total % perLevel;
        if (total >= max) {
            tooltip.add(Component.literal("● " + name + ": ").withStyle(Style.EMPTY.withColor(color)).append(Component.literal(activeDesc).withStyle(ChatFormatting.GREEN)));
        } else if (currentLevel > 0) {
            tooltip.add(Component.literal("● " + name + ": ").withStyle(Style.EMPTY.withColor(color))
                    .append(Component.literal(activeDesc).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + progressInLevel + "/" + perLevel + ")").withStyle(ChatFormatting.GRAY)));
        } else {
            tooltip.add(Component.literal("● " + name + ": ").withStyle(Style.EMPTY.withColor(color))
                    .append(Component.literal("未达成 (" + total + "/" + perLevel + ")").withStyle(ChatFormatting.RED)));
        }
    }
}