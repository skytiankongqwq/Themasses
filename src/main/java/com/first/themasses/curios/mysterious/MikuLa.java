package com.first.themasses.curios.mysterious;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;

/**
 * 饰品类：处理物品本身属性与 Curios 接口
 */
public class MikuLa extends Item implements ICurio {

    public MikuLa(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> this);
        return new ICapabilityProvider() {
            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CuriosCapability.ITEM) {
                    return (LazyOptional<T>) (Object) lazyCurio;
                }
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        // 1. 获取翻译后的原始字符串内容
        String rawText = Component.translatable("tooltip.themasses.mikula.line1").getString();

        // 2. 创建一个空的根组件用来承载所有字符
        MutableComponent coloredText = Component.empty();

        // 3. 遍历每个字符并设置颜色
        for (int i = 0; i < rawText.length(); i++) {
            char c = rawText.charAt(i);
            // 根据索引 i 的奇偶来决定颜色 (i%2 == 0 为偶数，i%2 != 0 为奇数)
            ChatFormatting color = (i % 2 == 0) ? ChatFormatting.WHITE : ChatFormatting.LIGHT_PURPLE;
            // 创建单个字符的组件并应用样式，然后追加到根组件上
            coloredText.append(Component.literal(String.valueOf(c)).withStyle(color));
        }
        // 4. 将拼接好的彩色组件添加到 tooltip 列表中
        tooltip.add(coloredText);
    }

    // ICurio 接口实现
    @Override public ItemStack getStack() { return ItemStack.EMPTY; }
    @Override public boolean canEquip(SlotContext slotContext) { return true; }
    @Override public boolean canUnequip(SlotContext slotContext) { return true; }
}