package com.first.themasses.item.prop;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
//昆仲物品
public class HarikiriMaru extends Item {
    public HarikiriMaru(Properties properties) {
        super(properties.stacksTo(1).durability(0));
    }

    // 设置长按动作
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW; // 借用弓箭的蓄力动作
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 40; // 长按 2 秒 (40 ticks) 触发
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // 检查冷却
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (entity instanceof Player player && !level.isClientSide) {
            // 1. 扣血至 3 点
            if (player.getHealth() > 3.0f) {
                player.setHealth(3.0f);
            }

            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3600, 5));//虚弱
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 3600, 1));//缓慢

            CompoundTag data = player.getPersistentData();
            data.putLong("SeppukuTime", level.getGameTime());
            data.putBoolean("IsSeppukuActive", true);

            double radius = 16.0;
            List<Mob> nearbyMobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(radius));

            for (Mob mob : nearbyMobs) {
                if (mob.getSensing().hasLineOfSight(player)) {

                    // 消除当前仇恨
                    mob.setTarget(null);
                    if (mob instanceof NeutralMob neutral) {
                        neutral.stopBeingAngry();
                    }

                    // 检查 Tag：介错判定
                    if (mob.getTags().contains("WitnessedSeppuku")) {
                        player.hurt(player.damageSources().fellOutOfWorld(), 99999f);
                        player.displayClientMessage(Component.literal("§c生物对你的懦弱感到愤怒，为你进行了介错！"), true);
                        mob.removeTag("WitnessedSeppuku");
                    } else {
                        mob.addTag("WitnessedSeppuku");
                    }
                }
            }

            player.displayClientMessage(Component.literal("§7腹肌被切开了！流出了不错的血液！"), true);
            player.getCooldowns().addCooldown(this, 2400);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.harariki.line1").withStyle(Style.EMPTY.withColor(0x9933FF)));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}