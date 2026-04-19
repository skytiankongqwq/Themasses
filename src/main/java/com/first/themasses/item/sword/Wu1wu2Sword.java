package com.first.themasses.item.sword;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Wu1wu2Sword extends SwordItem {

    public Wu1wu2Sword() {
        // 虽然基础攻击力设为 5，但因为攻击事件被取消，这个数值将仅作为面板展示
        super(Tiers.WOOD, 4, -2.4F, new Item.Properties().stacksTo(1).durability(1000));
    }

    /**
     * 核心逻辑：无论攻击什么，都拦截攻击事件
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        // 1. 只要点击的是生物（LivingEntity），我们就进行逻辑处理
        if (entity instanceof LivingEntity target) {

            // 如果目标是商人，执行窃取和降价逻辑
            if (target instanceof Merchant merchant && !player.level().isClientSide) {
                processMerchantLogic(stack, player, target, merchant);
            } else if (!player.level().isClientSide) {
                // 如果不是商人（比如僵尸），只播放一个粒子效果或轻微音效提示攻击无效
                // 也可以根据需要在这里加点别的动作，目前保持纯粹的“取消伤害”
                player.level().playSound(null, target.blockPosition(),
                        SoundEvents.ITEM_FRAME_BREAK, SoundSource.PLAYERS, 0.5F, 1.5F);
            }

            // 手动触发手臂挥动动画（因为返回 true 有时会拦截原版动画）
            player.swing(InteractionHand.MAIN_HAND, true);

            // 【绝对拦截】返回 true，攻击事件对所有生物失效
            return true;
        }

        return super.onLeftClickEntity(stack, player, entity);
    }

    /**
     * 封装商人专属逻辑
     */
    private void processMerchantLogic(ItemStack stack, Player player, LivingEntity target, Merchant merchant) {
        MerchantOffers offers = merchant.getOffers();

        if (offers != null && !offers.isEmpty()) {
            RandomSource random = player.getRandom();

            // 摸取物品
            MerchantOffer randomOffer = offers.get(random.nextInt(offers.size()));
            ItemStack dropItem = randomOffer.getResult().copy();

            ItemEntity itemEntity = new ItemEntity(
                    target.level(),
                    target.getX(), target.getY() + 0.5, target.getZ(),
                    dropItem
            );
            itemEntity.setDefaultPickUpDelay();
            target.level().addFreshEntity(itemEntity);

            // 治疗与降价
            target.heal(1.0F);
            for (MerchantOffer offer : offers) {
                ItemStack baseCost = offer.getBaseCostA();
                if (!baseCost.isEmpty()) {
                    int discount = Math.max(1, (int) Math.ceil(baseCost.getCount() * 0.05));
                    offer.addToSpecialPriceDiff(-discount);
                }
            }

            // 反馈音效
            target.level().playSound(null, target.blockPosition(),
                    SoundEvents.VILLAGER_TRADE, SoundSource.NEUTRAL, 1.0F, 1.0F);

            // 消耗耐久
            stack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = stack.getOrCreateTag();
        tooltip.add(Component.translatable("tooltip.themasses.bladeoftrade.line1").withStyle(ChatFormatting.RED));
        }
    }
