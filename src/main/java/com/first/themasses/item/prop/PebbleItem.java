package com.first.themasses.item.prop;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PebbleItem extends Item {
    public PebbleItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GRAVEL_BREAK, SoundSource.PLAYERS, 0.5F, 1.2F);

        if (!level.isClientSide) {
            RandomSource random = level.getRandom();
            double luck = player.getAttributeValue(Attributes.LUCK);

            // 1. 生成 0~100 的基础随机数（尚未添加幸运值）
            double rawRoll = random.nextDouble() * 100;

            // 2. 绝对保底机制：无视任何幸运值，只要基础值 < 10，必定爆炸（10% 强制爆炸概率）
            if (rawRoll < 10) {
                // 传入 luck 以便计算爆炸威力
                createMiniExplosion(level, player, luck);
            } else {
                // 3. 剩下的 90% 情况，将基础值加上幸运值进行正常判定
                // 【修改点】幸运修正：加成砍成原来的1/10，每 1 点幸运现在只增加 0.1% 的好运概率
                double roll = rawRoll + (luck * 0.1);

                if (roll > 90) {
                    // 10% 基础概率
                    giveOre(player, random, true);
                } else if (roll > 65) {
                    // 25% 基础概率
                    applyTrulyRandomEffect(player, random, luck);
                } else if (roll > 50) {
                    // 15% 基础概率
                    giveOre(player, random, false);
                } else {
                    // 剩下 40% 的区间，触发爆炸
                    createMiniExplosion(level, player, luck);
                }
            }

            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private void applyTrulyRandomEffect(Player player, RandomSource random, double luck) {
        List<MobEffect> allEffects = new ArrayList<>(BuiltInRegistries.MOB_EFFECT.stream().toList());
        MobEffect randomEffect = allEffects.get(random.nextInt(allEffects.size()));

        int amplifier = 0;
        // 【修改点】药水等级的幸运值加成也同步砍成 1/10
        double ampRoll = random.nextDouble() * 10 + (luck * 0.1);
        if (ampRoll > 12) amplifier = 2;
        else if (ampRoll > 7) amplifier = 1;

        player.addEffect(new MobEffectInstance(randomEffect, 600, amplifier));

        player.displayClientMessage(Component.literal("§b获得效果: ")
                .append(randomEffect.getDisplayName())
                .append(" " + (amplifier + 1) + "级"), true);
    }

    private void giveOre(Player player, RandomSource random, boolean isRare) {
        double luck = player.getAttributeValue(Attributes.LUCK);
        int maxCount = 1 + (int)Math.max(0, Math.floor(luck / 2.0));
        int count = 1 + random.nextInt(Math.max(1, maxCount + (isRare ? 1 : 2)));

        Item selectedItem;
        if (isRare) {
            // 【修改点】稀有物品强制上限 10 个
            count = Math.min(count, 10);
            List<Item> rarePool = List.of(
                    Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP,
                    Items.GOLD_INGOT, Items.AMETHYST_SHARD, Items.ECHO_SHARD,
                    Items.ANCIENT_DEBRIS, Items.NETHER_STAR
            );
            selectedItem = rarePool.get(random.nextInt(rarePool.size()));
        } else {
            // 【修改点】普通物品强制上限 64 个
            count = Math.min(count, 64);
            List<Item> commonPool = List.of(
                    Items.IRON_INGOT, Items.COPPER_INGOT, Items.RAW_GOLD,
                    Items.LAPIS_LAZULI, Items.COAL, Items.QUARTZ,
                    Items.REDSTONE, Items.GLOWSTONE_DUST, Items.RAW_IRON
            );
            selectedItem = commonPool.get(random.nextInt(commonPool.size()));
        }

        ItemStack stack = new ItemStack(selectedItem, count);
        player.addItem(stack);

        String prefix = isRare ? "§6§l[极好] §b发现 " : "§f[普通] §7捡到 ";
        player.displayClientMessage(Component.literal(prefix + count + "x ")
                .append(selectedItem.getDescription()), true);
    }

    private void createMiniExplosion(Level level, Player player, double luck) {
        // 1. 视觉爆炸：限制最大半径（如 4.0F），防止爆炸射线检测卡死服务器
        // ExplosionInteraction.NONE 确保这颗雷哪怕再大，也绝对不会破坏一丁点地形
        float visualRadius = Math.min(0.7F + (float)(luck * 0.05), 4.0F);
        level.explode(null, player.getX(), player.getY(), player.getZ(), visualRadius, false, ExplosionInteraction.NONE);

        // 2. 真实伤害计算：不再通过放大爆炸范围来算伤害，而是直接按比例对玩家追加扣血
        // 比例维持你的要求：每 1 点幸运值 = 额外增加 1.0 的伤害
        if (luck > 1.0) {
            float extraDamage = (float) luck * 1.0F;
            // 采用 1.20.1+ 的伤害源 API，直接对玩家造成数值伤害
            player.hurt(player.damageSources().generic(), extraDamage);
        }

        player.displayClientMessage(Component.literal("§c哎呀，错过好运啦 :P"), true);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.pebble.line1").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}