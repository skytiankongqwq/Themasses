package com.first.themasses.curios.mysterious;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * Duvacein - 幽密之影
 * 效果 1：幽密体 - 自动生成幽匿陷阱并召唤监守者
 * 效果 2：点金手 - 右键金块进行 3x3x3 感染
 */
public class Duvacein extends Item implements ICurioItem {

    public Duvacein(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // 使用匿名内部类实现 ICurio，这样可以正确引用当前的 ItemStack (stack)
        ICurio curioCapability = new ICurio() {
            @Override
            public ItemStack getStack() {
                // 重要：必须返回当前的 stack 实例，Curios API 才能通过物品检测
                return stack;
            }

            @Override
            public void curioTick(SlotContext slotContext) {
                // 逻辑已由 DuvaceinEvents 类接管，此处保持精简
            }

            @Override
            public boolean canEquip(SlotContext slotContext) {
                return true;
            }

            @Override
            public boolean canUnequip(SlotContext slotContext) {
                return true;
            }
        };

        return new ICapabilityProvider() {
            private final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> curioCapability);

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                // 正确注入 Curios 能力
                if (cap == CuriosCapability.ITEM) {
                    return lazyCurio.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // 添加翻译文本描述
        Component line1 = Component.translatable("tooltip.themasses.Duvacein.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA)));

        tooltip.add(line1);

        // 额外提示（可选）
        tooltip.add(Component.literal("§8[幽密体] §7在周围布下幽匿陷阱"));
        tooltip.add(Component.literal("§8[点金手] §7右键金块传播黄金感染"));
    }
}