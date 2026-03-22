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
import net.minecraft.world.effect.MobEffects;
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

            // 幸运修正：每 1 点幸运增加 10% 的好运概率
            double roll = random.nextDouble() * 100 + (luck * 10);

            if (roll > 85) {
                // 极好：稀有矿石
                giveOre(player, random, true);
            } else if (roll > 45) {
                // 良好：全随机药水效果
                applyTrulyRandomEffect(player, random, luck);
            } else if (roll > 20) {
                // 普通：普通矿石
                giveOre(player, random, false);
            } else {
                // 倒霉：微弱爆炸
                createMiniExplosion(level, player);
            }

            if (!player.getAbilities().instabuild) {
                itemstack.shrink(1);
            }
        }

        return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
    }

    private void applyTrulyRandomEffect(Player player, RandomSource random, double luck) {
        // 从注册表中获取所有效果
        List<MobEffect> allEffects = new ArrayList<>(BuiltInRegistries.MOB_EFFECT.stream().toList());
        MobEffect randomEffect = allEffects.get(random.nextInt(allEffects.size()));

        // 等级逻辑：基础 1 级。如果幸运值 > 2，有概率升为 2 级；幸运 > 5，有概率升为 3 级。
        int amplifier = 0; // 0 代表一级
        double ampRoll = random.nextDouble() * 10 + luck;
        if (ampRoll > 12) amplifier = 2; // III级
        else if (ampRoll > 7) amplifier = 1; // II级

        player.addEffect(new MobEffectInstance(randomEffect, 600, amplifier));
        player.sendSystemMessage(Component.literal("§b获得效果: " + randomEffect.getDisplayName().getString() + " " + (amplifier + 1) + "级"));
    }

    private void giveOre(Player player, RandomSource random, boolean isRare) {
        // 1. 获取玩家幸运值，用于增加产出数量
        double luck = player.getAttributeValue(Attributes.LUCK);

        // 基础数量 1，每有 2 点幸运值，最大可能数量就 +1
        int maxCount = 1 + (int)Math.max(0, Math.floor(luck / 2.0));
        int count = 1 + random.nextInt(Math.max(1, maxCount + (isRare ? 1 : 2)));

        // 2. 扩充后的矿石池
        Item selectedItem;
        if (isRare) {
            // 稀有池
            List<Item> rarePool = List.of(
                    Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP,
                    Items.GOLD_INGOT, Items.AMETHYST_SHARD, Items.ECHO_SHARD,
                    Items.ANCIENT_DEBRIS, Items.NETHER_STAR
            );
            selectedItem = rarePool.get(random.nextInt(rarePool.size()));
        } else {
            // 普通池
            List<Item> commonPool = List.of(
                    Items.IRON_INGOT, Items.COPPER_INGOT, Items.RAW_GOLD,
                    Items.LAPIS_LAZULI, Items.COAL, Items.QUARTZ,
                    Items.REDSTONE, Items.GLOWSTONE_DUST, Items.RAW_IRON
            );
            selectedItem = commonPool.get(random.nextInt(commonPool.size()));
        }

        // 3. 给予物品
        ItemStack stack = new ItemStack(selectedItem, count);
        player.addItem(stack);

        // 4. 提示信息优化
        String message = isRare ? "§6§l[极好] §b你发现了 " : "§f[普通] §7你捡到了 ";
        player.displayClientMessage(Component.literal(message + count + "x " + selectedItem.getDescription().getString()), true);
    }

    private void createMiniExplosion(Level level, Player player) {
        // 降低威力到 0.5F（几乎不掉血），Interaction.NONE 确保不破坏方块
        level.explode(null, player.getX(), player.getY(), player.getZ(), 0.7F, false, ExplosionInteraction.NONE);
        player.sendSystemMessage(Component.literal("§c哎呀！碎石炸了..."));
    }
    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.pebble.line1").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}