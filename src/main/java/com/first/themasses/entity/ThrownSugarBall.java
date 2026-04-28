package com.first.themasses.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class ThrownSugarBall extends ThrowableItemProjectile {
    public ThrownSugarBall(EntityType<? extends ThrownSugarBall> type, Level level) {
        super(type, level);
    }

    public ThrownSugarBall(Level level, LivingEntity shooter) {
        super(EntityType.SNOWBALL, shooter, level); // 暂时借用雪球类型或自定义
    }

    @Override
    protected Item getDefaultItem() {
        return Items.SUGAR; // 这里稍后改成你注册的 SugarBall 物品
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            // 产生糖粉粒子效果
            ((ServerLevel)this.level()).sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY(), this.getZ(), 10, 0.5, 0.5, 0.5, 0.1);
            this.level().broadcastEntityEvent(this, (byte)3);

            // 执行 3x3 范围效果
            applyAreaEffect();

            this.discard();
        }
    }

    private void applyAreaEffect() {
        BlockPos center = this.blockPosition();
        Level level = this.level();

        // 1. 催熟庄稼和树木 (3x3)
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock growable) {
                // 循环催熟直到无法继续成长（直接完成）
                for (int i = 0; i < 8; i++) {
                    if (growable.isValidBonemealTarget(level, pos, state, level.isClientSide)) {
                        growable.performBonemeal((ServerLevel) level, level.random, pos, state);
                        state = level.getBlockState(pos); // 更新状态继续检查
                    } else {
                        break;
                    }
                }
            }
        }

        // 2. 回复玩家饥饿度 (3x3 范围实体)
        AABB area = new AABB(this.getX() - 1.5, this.getY() - 1.5, this.getZ() - 1.5, this.getX() + 1.5, this.getY() + 1.5, this.getZ() + 1.5);
        level.getEntitiesOfClass(Player.class, area).forEach(player -> {
            player.getFoodData().eat(6, 0.6F); // 6点饥饿度=3格，0.6饱和度
            player.playSound(SoundEvents.PLAYER_BURP, 0.5F, 1.0F);
        });
    }
}