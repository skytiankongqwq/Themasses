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
import net.minecraft.world.phys.Vec3;
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
                giveOre(player, random, true);
            } else if (roll > 45) {
                applyTrulyRandomEffect(player, random, luck);
            } else if (roll > 20) {
                giveOre(player, random, false);
            } else {
                createMiniExplosion(level, player);
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
        double ampRoll = random.nextDouble() * 10 + luck;
        if (ampRoll > 12) amplifier = 2;
        else if (ampRoll > 7) amplifier = 1;

        player.addEffect(new MobEffectInstance(randomEffect, 600, amplifier));

        // 修改点：使用 displayClientMessage(..., true) 移动到屏幕下方
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
            List<Item> rarePool = List.of(
                    Items.DIAMOND, Items.EMERALD, Items.NETHERITE_SCRAP,
                    Items.GOLD_INGOT, Items.AMETHYST_SHARD, Items.ECHO_SHARD,
                    Items.ANCIENT_DEBRIS, Items.NETHER_STAR
            );
            selectedItem = rarePool.get(random.nextInt(rarePool.size()));
        } else {
            List<Item> commonPool = List.of(
                    Items.IRON_INGOT, Items.COPPER_INGOT, Items.RAW_GOLD,
                    Items.LAPIS_LAZULI, Items.COAL, Items.QUARTZ,
                    Items.REDSTONE, Items.GLOWSTONE_DUST, Items.RAW_IRON
            );
            selectedItem = commonPool.get(random.nextInt(commonPool.size()));
        }

        ItemStack stack = new ItemStack(selectedItem, count);
        player.addItem(stack);

        // 修改点：确保提示信息统一出现在屏幕下方
        String prefix = isRare ? "§6§l[极好] §b发现 " : "§f[普通] §7捡到 ";
        player.displayClientMessage(Component.literal(prefix + count + "x ")
                .append(selectedItem.getDescription()), true);
    }

    private void createMiniExplosion(Level level, Player player) {
        level.explode(null, player.getX(), player.getY(), player.getZ(), 0.7F, false, ExplosionInteraction.NONE);

        // 修改点：倒霉提示也移到屏幕下方
        player.displayClientMessage(Component.literal("§c哎呀，错过好运啦 :P"), true);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.pebble.line1").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}