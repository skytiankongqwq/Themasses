package com.first.themasses.item.prop;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.PlayerTeam;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.List;

public class SummonerScythe extends Item {

    public SummonerScythe(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            CompoundTag nbt = stack.getOrCreateTag();
            int charges = nbt.getInt("Charges");

            // 判断消耗数量
            int toConsume = player.isShiftKeyDown() ? 10 : 1;

            if (charges >= toConsume) {
                // 召唤逻辑
                for (int i = 0; i < toConsume; i++) {
                    spawnFriendlyMob(serverLevel, player);
                }

                // 扣除次数
                nbt.putInt("Charges", charges - toConsume);
                player.displayClientMessage(Component.literal("§a已召唤 " + toConsume + " 只友军！剩余次数: " + (charges - toConsume)), true);
            } else {
                player.displayClientMessage(Component.literal("§c能量不足！当前储能: " + charges), true);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private void spawnFriendlyMob(ServerLevel level, Player player) {
        Random rand = new Random();

        // 1. 随机生物池：增加多样性
        EntityType<?>[] pool = {
                EntityType.WOLF,
                EntityType.CAT,
                EntityType.PARROT,
                EntityType.IRON_GOLEM,
                EntityType.SNOW_GOLEM,
                EntityType.LLAMA
        };

        EntityType<?> selectedType = pool[rand.nextInt(pool.length)];

        // 2. 计算随机坐标：玩家周围 8x8 范围内随机偏移
        double offsetX = (rand.nextDouble() - 0.5) * 8.0;
        double offsetZ = (rand.nextDouble() - 0.5) * 8.0;
        double spawnX = player.getX() + offsetX;
        double spawnZ = player.getZ() + offsetZ;
        double spawnY = player.getY(); // 基础高度与玩家持平

        // 3. 生成实体
        Entity entity = selectedType.spawn(level, (ItemStack) null, null, player.blockPosition().offset((int)offsetX, 0, (int)offsetZ), MobSpawnType.EVENT, true, false);

        if (entity instanceof LivingEntity living) {
            // A. 设置主人逻辑 (针对狼、猫、鹦鹉等可驯服生物)
            if (living instanceof TamableAnimal tamable) {
                tamable.tame(player);
                tamable.setOrderedToSit(false); // 确保它们生成后是站立状态跟随
            }

            // B. 针对铁傀儡/雪傀儡等不可驯服但有归属感的生物
            // 我们可以通过原版 Team 系统将它们与玩家绑定，防止误伤
            if (player.getTeam() != null) {
                level.getScoreboard().addPlayerToTeam(living.getScoreboardName(), (PlayerTeam) player.getTeam());
            }

            // C. 视觉反馈：在每个生物生成的点刷一点烟雾
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                    living.getX(), living.getY() + 0.5, living.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = stack.getOrCreateTag();
        int kills = nbt.getInt("Kills");
        int charges = nbt.getInt("Charges");

        tooltip.add(Component.literal("击杀进度: " + kills + "/10").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("可用召唤次数: " + charges + "/1124").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable("tooltip.themasses.summoner.line1").withStyle(ChatFormatting.DARK_PURPLE));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}