package com.first.themasses.entity;

import com.first.themasses.effect.ModEffects;
import com.first.themasses.entity.ModEntities;
import com.first.themasses.item.ModItems;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

public class VoidPiercerProjectileEntity extends AbstractArrow {

    private int blockHitTimer = -1;

    public VoidPiercerProjectileEntity(EntityType<? extends AbstractArrow> type, Level level) {
        super(type, level);
        this.setNoGravity(true); // 核心：无视重力
    }

    public VoidPiercerProjectileEntity(Level level, LivingEntity shooter) {
        super(ModEntities.VOID_PIERCER_PROJECTILE.get(), shooter, level);
        this.setNoGravity(true);
        // 设置伤害，10点 = 5心
        this.setBaseDamage(10.0D);
    }


    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (result.getEntity() instanceof LivingEntity target) {
            // 1. 获取射击者
            Entity owner = this.getOwner();

            // 2. 构建伤害来源：确保 LivingHurtEvent 里的环境/饰品加成能识别到玩家
            DamageSource source = owner instanceof Player player ?
                    player.damageSources().arrow(this, player) :
                    this.damageSources().arrow(this, owner);

            // 3. 赋予紫昙花 Buff (1级，30秒)
            // 逻辑：只有当目标身上没有这个 Buff 时才给予
            if (!target.hasEffect(ModEffects.ZITANHUA.get())) {
                target.addEffect(new MobEffectInstance(ModEffects.ZITANHUA.get(), 600, 0));
            }

            // 4. 造成固定 10.0F 基础伤害
            // 因为取消了无敌帧，如果有多发投影同时命中，现在每一发都能实打实造成伤害
            target.hurt(source, 10.0F);

            // 5. 🛡️ 软依赖效果：深渊腐蚀
            applyAbyssCorruption(target);

            // 6. 击中后处理：投影消失
            if (!this.level().isClientSide) {
                this.discard();
            }
        }
    }

    /**
     * 安全地应用深渊腐蚀效果 (软依赖)
     */
    private void applyAbyssCorruption(LivingEntity target) {
        if (ModList.get().isLoaded("enigmaticdelicacy")) {
            MobEffect abyssCorruption = ForgeRegistries.MOB_EFFECTS.getValue(
                    new ResourceLocation("enigmaticdelicacy", "abyss_corruption")
            );
            if (abyssCorruption != null) {
                // 30秒 = 600 ticks
                target.addEffect(new MobEffectInstance(abyssCorruption, 600, 0));
            }
        }
    }

    @Override
    protected @NotNull ItemStack getPickupItem() {
        // 投影不可回收
        return ItemStack.EMPTY;
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        // 必须重写这个，否则客户端无法渲染自定义实体
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    @Override
    protected void onHitBlock(BlockHitResult result) {
        // 不要调用 super.onHitBlock(result)，除非你想让它像箭一样插在方块上
        // 我们只需要记录撞击发生的时刻
        if (this.blockHitTimer == -1) {
            this.blockHitTimer = 0; // 开始计时
        }
    }

    @Override
    public void tick() {
        super.tick();

        // 如果已经撞到了方块，开始累加计时器
        if (this.blockHitTimer >= 0) {
            this.blockHitTimer++;

            // 1秒 = 20 ticks
            if (this.blockHitTimer >= 20) {
                if (!this.level().isClientSide) {
                    this.discard(); // 消失
                }
            }
        }

        // 额外的保险：如果飞得太远（例如 10 秒没撞到任何东西），也让它消失，防止占用服务器资源
        if (this.tickCount > 200 && !this.level().isClientSide) {
            this.discard();
        }

        // 可以在这里添加 tick 方法来生成紫色粒子轨迹
    }
}