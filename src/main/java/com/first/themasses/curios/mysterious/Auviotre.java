package com.first.themasses.curios.mysterious;

import com.first.themasses.item.curios.CurioBaseBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
//auv饰品
//1.初始有20的护盾，可以抵挡等额伤害，通过紫水晶充能（死了也充能qwq）
//2.玩家在100-150格高且头上没有遮挡的晚上（13000tick到20000tick）这个期间获得80%的伤害减免，
//3.踩在紫水晶方块上持续获得瞬间治疗1
//4.免疫摔落伤害，在5格以上高度后坠地时对周围生物造成(落地距离-5)×2的伤害
//5加个击杀生物60%掉落1-3个紫水晶
//6攻击时70%出现小行星
//7.夜晚攻击的目标会获得发光buff，攻击有发光buff的生物伤害增加50%，白天伤害减少-20%,如果自己有发光buff，则持续时间内伤害减免10%
import java.util.List;
//1.初始有20的护盾，可以抵挡等额伤害，通过紫水晶充能（死了也充能qwq）
//2.玩家在100-150格高且头上没有遮挡的晚上（13000tick到20000tick）这个期间获得80%的伤害减免，
//3.踩在紫水晶方块上持续获得瞬间治疗1
//4.免疫摔落伤害，在5格以上高度后坠地时对周围生物造成(落地距离-5)×2的伤害
//5加个击杀生物60%掉落1-3个紫水晶
//6攻击时70%出现小行星
//7.夜晚攻击的目标会获得发光buff，攻击有发光buff的生物伤害增加50%，白天伤害减少-20%
//如果自己有发光buff，则持续时间内伤害减免10%
public class Auviotre extends CurioBaseBlockItem {

    public Auviotre(Block block, Properties properties) {
        super(block, properties);
    }

    // --- Curios Tick 逻辑 ---
    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        Level level = entity.level();

        // 初始化护盾
        if (!stack.getOrCreateTag().contains("auviotre_shield")) {
            setShield(stack, 20.0F);
        }

        if (!level.isClientSide) {
            // --- 3. 踩在紫水晶方块获得瞬间治疗1 ---
            BlockPos pos = entity.blockPosition().below();
            if (level.getBlockState(pos).is(Blocks.AMETHYST_BLOCK)) {
                if (entity.tickCount % 4 == 0) { // 每4tick触发一次
                    entity.addEffect(new MobEffectInstance(MobEffects.HEAL, 1, 0, false, false));
                }
            }
        }
    }

    // --- 护盾辅助方法 ---
    public static float getShield(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        return tag.contains("auviotre_shield") ? tag.getFloat("auviotre_shield") : 20.0F;
    }

    public static void setShield(ItemStack stack, float value) {
        stack.getOrCreateTag().putFloat("auviotre_shield", Math.max(0, Math.min(value, 20.0F)));
    }

    // --- 伤害减免逻辑 (供事件类调用) ---
    public static float handleDamage(LivingEntity entity, ItemStack stack, float amount) {
        Level level = entity.level();
        long time = level.getDayTime() % 24000L;

        // 2. 100-150格高度 + 露天 + 晚上(13000-20000) 80%减免
        if (entity.getY() >= 100 && entity.getY() <= 150 && level.canSeeSky(entity.blockPosition()) && time >= 13000 && time <= 20000) {
            amount *= 0.2F;
        }

        // 新增：如果自己有发光buff，伤害减免10%
        if (entity.hasEffect(MobEffects.GLOWING)) {
            amount *= 0.9F;
        }

        // 1. 护盾抵扣
        float shield = getShield(stack);
        if (shield > 0) {
            if (shield >= amount) {
                setShield(stack, shield - amount);
                return 0;
            } else {
                float remaining = amount - shield;
                setShield(stack, 0);
                return remaining;
            }
        }
        return amount;
    }

    // --- 新增：攻击逻辑处理 (供事件类调用) ---
    public static float handleAttack(LivingEntity attacker, LivingEntity target, float amount) {
        Level level = attacker.level();
        long time = level.getDayTime() % 24000L;
        boolean isNight = time >= 13000 && time <= 23000;
        boolean isDay = time < 12000 || time > 23000;

        // 夜晚攻击目标获得发光buff
        if (isNight && !level.isClientSide) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
        }

        // 攻击有发光buff的生物伤害增加50%
        if (target.hasEffect(MobEffects.GLOWING)) {
            amount *= 1.5F;
        }

        // 白天伤害减少20%
        if (isDay) {
            amount *= 0.8F;
        }

        return amount;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        float shield = getShield(stack);
        tooltip.add(Component.translatable("tooltip.themasses.Auviotre.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9933FF))));
        // 调用父类的 appendHoverText 保持链条完整
        super.appendHoverText(stack, level, tooltip, flag);
    }
}