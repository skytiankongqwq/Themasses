package com.first.themasses.item.sword;

import com.first.themasses.effect.ModEffects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeMod;
import org.jetbrains.annotations.NotNull;
import com.first.themasses.entity.VoidPiercerProjectileEntity;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

import static com.first.themasses.curios.mysterious.Auviotre.getShield;

public class VoidPiercer extends SwordItem {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    // 修改构造函数，只接收 Properties，其余参数在内部写死
    public VoidPiercer(Properties properties) {
        // 使用 Tiers.NETHERITE (下界合金等级)，伤害设为 0，攻速设为 -2.4F
        super(Tiers.NETHERITE, 0, -2.4F, properties);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        // 15点总伤害 (1.0 基础 + 14.0 修正)
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 14.0D, AttributeModifier.Operation.ADDITION));
        // 1.6 攻击速度 (4.0 基础 - 2.4 修正)
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4D, AttributeModifier.Operation.ADDITION));
        // 攻击距离 +3
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(UUID.fromString("C6AAD2DA-B19F-4A94-9D69-8079C39E9623"), "Range modifier", 3.0D, AttributeModifier.Operation.ADDITION));
        builder.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(UUID.fromString("C6AAD2DA-B19F-4A94-9D69-8079C39E9623"), "Range modifier", 3.0D, AttributeModifier.Operation.ADDITION));

        this.defaultModifiers = builder.build();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // 赋予它格挡的动作动画（虽然渲染被你接管了，但逻辑上必须是 BLOCK）
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // 设置格挡的最大持续时间（设置一个很大的值，比如 72000）
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // --- 功能 1：蹲下右键扔出武器投影 ---
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                VoidPiercerProjectileEntity projectile = new VoidPiercerProjectileEntity(level, player);
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 6.0F, 0.0F);
                level.addFreshEntity(projectile);
                itemstack.hurtAndBreak(5, player, (p) -> p.broadcastBreakEvent(hand));
            }
            return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
        }

        // --- 功能 2：格挡与冷却逻辑 ---

        // 检查是否佩戴了 laowang237 饰品
        boolean hasJewelry = CuriosApi.getCuriosHelper().findFirstCurio(player, stack ->
                stack.getItem().toString().contains("laowang237")
        ).isPresent();

        // 如果没有饰品，则检查并设置冷却
        if (!hasJewelry) {
            // 2.37s * 20t/s = 47.4 -> 取 47 ticks
            player.getCooldowns().addCooldown(this, 47);
        }

        // 开始使用物品（进入格挡状态）
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemstack);
    }
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 1. 检查目标是否已经拥有 zitanhua Buff
        if (!target.hasEffect(ModEffects.ZITANHUA.get())) {
            // 2. 如果没有，给予一级（amplifier为0），持续30秒（600 ticks）的Buff
            target.addEffect(new MobEffectInstance(ModEffects.ZITANHUA.get(), 600, 0));
        }

        // 调用父类逻辑处理耐久消耗等
        return super.hurtEnemy(stack, target, attacker);
    }


    // --- 核心：附魔多类型兼容 ---
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        EnchantmentCategory category = enchantment.category;
        // 兼容：剑、刀（通常是剑类）、弓、三叉戟
        return category == EnchantmentCategory.WEAPON ||
                category == EnchantmentCategory.BOW ||
                category == EnchantmentCategory.TRIDENT ||
                category == EnchantmentCategory.BREAKABLE; // 包含耐久等常规
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        float shield = getShield(stack);
        tooltip.add(Component.translatable("tooltip.themasses.zitanhua.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9933FF))));
        // 调用父类的 appendHoverText 保持链条完整
        super.appendHoverText(stack, level, tooltip, flag);
    }
}