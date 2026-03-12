package com.first.themasses.item.food;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
//伊甸禁果的部分逻辑
public class ForbiddenFruitItem extends Item {

    private static final String MOD_DATA_TAG = "themasses_data";
    private static final String KILL_COUNT_TAG = "ForbiddenFruitKillCount";
    private static final String KILL_TIMER_TAG = "ForbiddenFruitKillTimer";

    public ForbiddenFruitItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, @NotNull TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(
                Component.translatable("item.themasses.forbidden_fruit.tooltip")
                        .withStyle(ChatFormatting.DARK_PURPLE)
        );
        super.appendHoverText(pStack, pLevel, pTooltipComponents, pIsAdvanced);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        // 只在服务端处理逻辑
        if (!level.isClientSide && livingEntity instanceof ServerPlayer serverPlayer) {

            // 1. 发送诅咒起始文本 (不再在这里给经验)
            serverPlayer.sendSystemMessage(Component.literal("你被逐出了乐园，那知识即是诅咒")
                    .withStyle(ChatFormatting.DARK_PURPLE));

            // 2. 初始化惩罚状态 NBT
            CompoundTag playerData = serverPlayer.getPersistentData();
            CompoundTag customTag = playerData.getCompound(MOD_DATA_TAG);

            // 设置执行次数为 3 次
            // 计时器设为 1，确保在下一个 Server Tick 立即触发第一次 kill 判定
            customTag.putInt(KILL_COUNT_TAG, 3);
            customTag.putInt(KILL_TIMER_TAG, 60);

            playerData.put(MOD_DATA_TAG, customTag);
        }

        return super.finishUsingItem(stack, level, livingEntity);
    }
}