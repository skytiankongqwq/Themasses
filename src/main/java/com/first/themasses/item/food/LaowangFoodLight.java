package com.first.themasses.item.food;

import com.first.themasses.TheMasses;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;

import javax.annotation.Nullable;
import java.util.List;

//老王食物
public class LaowangFoodLight extends Item {

    public LaowangFoodLight(Properties pProperties) {
        super(pProperties);
    }
    // 💥 增加：重写 appendHoverText 方法以添加描述
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        // 描述行 1: 主要描述
        Component line1 = Component.translatable(
                "tooltip.themasses.laowang237_food.line1"
        ).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x5555FF))); // 蓝色文本

        tooltip.add(line1);
    }

    // 关键方法：在玩家吃完食物时调用
    @Override
    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        super.finishUsingItem(pStack, pLevel, pLivingEntity);

        if (pLevel instanceof ServerLevel serverLevel) {
            double x = pLivingEntity.getX();
            double y = pLivingEntity.getY();
            double z = pLivingEntity.getZ();

            LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);

            if (lightning != null) {
                lightning.moveTo(x, y, z);
                serverLevel.addFreshEntity(lightning);
                TheMasses.LOGGER.info("Lightning summoned by Laowang Food at {}, {}, {}", x, y, z);
            }
        }



        return pStack;
    }
}
