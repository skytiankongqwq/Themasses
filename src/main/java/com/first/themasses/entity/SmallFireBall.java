package com.first.themasses.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.List;

public class SmallFireBall extends Projectile implements ItemSupplier {
    // 增加一个成员变量来锁定目标
    @Nullable
    private LivingEntity trackedTarget;

    public SmallFireBall(EntityType<? extends Projectile> type, Level world) {
        super(type, world);
    }

    public SmallFireBall(Level world, LivingEntity owner, LivingEntity target) {
        this(com.first.themasses.entity.ModEntities.SMALLFIRE_BALL.get(), world);
        this.setOwner(owner);
        this.trackedTarget = target; // 锁定目标

        // 初始生成在目标头顶
        this.setPos(target.getX(), target.getY() + 10.0D, target.getZ());
        // 给一个向下的高速初速度
        this.setDeltaMovement(0, -0.6D, 0);
    }

    @Override
    public void tick() {
        super.tick();

        // 1. 客户端粒子效果
        if (this.level().isClientSide) {
            for (int i = 0; i < 2; ++i) {
                this.level().addParticle(ParticleTypes.FLAME, this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }

        // 2. 服务端追踪逻辑（核心改进）
        if (!this.level().isClientSide) {
            // 如果目标死亡或无效，火球直接消失，避免卡在空中
            if (trackedTarget == null || !trackedTarget.isAlive() || trackedTarget.isRemoved()) {
                this.discard();
            } else {
                // 强制将火球的 X 和 Z 坐标直接同步为目标的坐标
                double targetX = trackedTarget.getX();
                double targetZ = trackedTarget.getZ();

                // 使用 moveTo 确保位置更新能同步给客户端
                this.moveTo(targetX, this.getY(), targetZ);
            }
        }

        // 3. 碰撞与运动处理
        Vec3 vec3 = this.getDeltaMovement();
        HitResult hitresult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);

        // 增加一点：如果没撞到东西，但火球已经低于目标位置了，强制触发命中
        if (trackedTarget != null && this.getY() <= trackedTarget.getY() + 0.5D) {
            this.onHitEntity(new EntityHitResult(trackedTarget));
        }

        if (hitresult.getType() != HitResult.Type.MISS && !ForgeEventFactory.onProjectileImpact(this, hitresult)) {
            this.onHit(hitresult);
        }

        // 注意：这里只更新 Y 轴的运动，XZ 已经在上面 moveTo 锁定了
        this.setPos(this.getX(), this.getY() + vec3.y, this.getZ());

        // 重力加速：让它越砸越快
        if (!this.isNoGravity()) {
            this.setDeltaMovement(vec3.add(0.0D, -0.06F, 0.0D));
        }

        if (this.getY() < this.level().getMinBuildHeight() || this.isInWater()) {
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        Entity target = hitResult.getEntity();
        Entity owner = this.getOwner();

        if (target instanceof LivingEntity livingTarget) {
            if (livingTarget.hurt(this.damageSources().indirectMagic(this, owner), 4.0F)) {
                livingTarget.setSecondsOnFire(3);
                this.level().playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        this.discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        if (!this.level().isClientSide) {
            // 落地补偿：即使没砸中头，砸中脚下也能造成小范围伤害
            AABB splashArea = this.getBoundingBox().inflate(1.5D);
            List<LivingEntity> nearby = this.level().getEntitiesOfClass(LivingEntity.class, splashArea);
            for (LivingEntity e : nearby) {
                if (e != this.getOwner()) {
                    e.hurt(this.damageSources().indirectMagic(this, this.getOwner()), 2.0F);
                    e.setSecondsOnFire(2);
                }
            }
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData() {}

    @Override
    public ItemStack getItem() {
        return new ItemStack(Items.FIRE_CHARGE);
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        this.setDeltaMovement(packet.getXa(), packet.getYa(), packet.getZa());
    }
}