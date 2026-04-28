package com.first.themasses.curios.mysterious;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
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

public class Sim extends Item implements ICurio {

    public Sim(Properties properties) {
        super(properties);
    }

    // 1. 核心：Capability 注入
    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> this);
        return new ICapabilityProvider() {
            @Override
            @SuppressWarnings("unchecked")
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                if (cap == CuriosCapability.ITEM) {
                    return (LazyOptional<T>) (Object) lazyCurio;
                }
                return LazyOptional.empty();
            }
            public void invalidateCaps() {
                lazyCurio.invalidate();
            }
        };
    }

    // 主动技能：仿真 (右键使用)
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            // 给玩家打上 NBT 标记，记录 Buff 结束的时间戳 (当前时间 + 60秒 = 1200 ticks)
            player.getPersistentData().putLong("SimActiveEndTime", level.getGameTime() + 1200);
            player.displayClientMessage(Component.literal("§b[仿真] §f视野推演已启动！"), true);

            // 随便加个冷却时间，这里设置 120 秒，避免无限用
            player.getCooldowns().addCooldown(this, 2400);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // --- 1. 添加普通描述文本 ---
        Component line1 = Component.translatable(
                "tooltip.themasses.Sim.line1"
        ).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA)));

        tooltip.add(line1);

    }

    @Override
    public ItemStack getStack() { return ItemStack.EMPTY; }
    @Override
    public boolean canEquip(SlotContext slotContext) { return true; }
    @Override
    public boolean canUnequip(SlotContext slotContext) { return true; }
    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack) { }
    @Override
    public void onUnequip(SlotContext slotContext, ItemStack newStack) { }
    @Override
    public void curioTick(SlotContext slotContext) { }
}