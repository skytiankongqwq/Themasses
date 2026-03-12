package com.first.themasses.curios.mysterious;

import com.first.themasses.curios.IronsCompat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

// 改为实现 ICurioItem 接口，而不是继承第三方基类
public class DaZhi extends Item implements ICurioItem {

    private static final UUID REACH_UUID = UUID.fromString("b72e1c9d-5555-4b2a-a92e-336c5332f144");

    public DaZhi(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        // 只有当 Irons Spellbooks 加载时才执行魔法逻辑
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsCompat.onEquip(slotContext, stack);
        }
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> attributes = HashMultimap.create();

        // 原版/Forge 属性不受影响
        attributes.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(REACH_UUID, "dazhi_reach", 5.0, AttributeModifier.Operation.ADDITION));

        // 外部魔法属性加载
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsCompat.addMagicAttributes(attributes);
        }

        return attributes;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsCompat.imbueSpell(stack);
        }
        return super.initCapabilities(stack, nbt);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (ModList.get().isLoaded("irons_spellbooks") && slotContext.entity().tickCount % 40 == 0) {
            IronsCompat.imbueSpell(stack);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.dazhi.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x8A2BE2))));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}