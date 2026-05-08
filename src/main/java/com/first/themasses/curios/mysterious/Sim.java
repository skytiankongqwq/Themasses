package com.first.themasses.curios.mysterious;

import com.first.themasses.networking.ModMessages;
import com.first.themasses.networking.PacketSyncSimTime;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerPlayer;
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
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;

@SuppressWarnings("removal")
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

        // 逻辑：只有在服务器端运行
        if (!level.isClientSide) {
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, this).isPresent()) {
                if (!player.getCooldowns().isOnCooldown(this)) {
                    Sim.startSimulation(player, level);
                    // 修改为 360 秒冷却 (7200 ticks)
                    player.getCooldowns().addCooldown(this, 7200);
                    return InteractionResultHolder.consume(stack);
                }
            } else {
                // 如果没戴在饰品栏，提示玩家
                player.displayClientMessage(Component.literal("§c[Sim] 必须佩戴在饰品栏才能激活仿真系统！"), true);
            }
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void startSimulation(Player player, Level level) {
        if (!level.isClientSide) {
            // 修改：标记技能开启。不再记录 EndTime，而是记录一个开关
            player.getPersistentData().putBoolean("SimLuckyActive", true);

            // 发送提示
            player.displayClientMessage(Component.literal("§b[仿真] §f因果逻辑已锁定：下一次 击杀/附魔/开箱 将获得全部可能收益！"), false);
        }
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