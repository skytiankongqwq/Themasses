package com.first.themasses.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth; // 用于三角函数计算
import net.minecraft.world.phys.Vec3; // 用于向量计算

import java.util.function.Predicate;
//烟花苦力怕的逻辑
public class FlashCreeper extends Creeper {

    // 爆炸距离的平方（不再使用）
    // private static final double FUSE_DISTANCE_SQ = 4.0D; // 移除未使用的常量

    // 瞬移目标距离：在玩家前方 1.5 格生成
    private static final double TELEPORT_DISTANCE = 1.5D;

    public FlashCreeper(EntityType<? extends Creeper> type, Level level) {
        super(type, level);
    }

    // --- AI 逻辑：瞬移机制 ---

    /**
     * 重写 registerGoals()：移除寻路和追逐，只保留基础动作。
     */
    @Override
    protected void registerGoals() {
        // 彻底移除所有寻路和追逐 AI
        this.goalSelector.removeAllGoals((Predicate<Goal>) (goal) -> true);
        this.targetSelector.removeAllGoals((Predicate<Goal>) (goal) -> true);

        // 仅保留最基础的动作（如头部转动），防止实体完全冻结。
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    // --- 核心机制重写 ---

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    /**
     * aiStep()：实现瞬移追踪和【立即爆炸】逻辑。
     */
    @Override
    public void aiStep() {
        super.aiStep();

        // 瞬移逻辑针对最近的玩家 (16格范围内)
        Player player = this.level().getNearestPlayer(this, 16.0D);

        if (player != null) {

            // 1. 检查玩家是否“看着”它
            if (this.doesPlayerLookAtMe(player)) {

                // 2. 瞬移到玩家面前
                this.teleportToPlayer(player);

                // 3. 【关键修正】：瞬移后立即爆炸，实现“瞬间”效果。
                this.explodeCreeper();

                // 注意：因为 explodeCreeper() 内部调用了 this.discard()，实体会被移除。
                // 因此后续的 else 块不会被执行。
            } else {
                // 玩家停止看着它，取消膨胀状态
                this.setSwellDir(-1);
            }

        } else {
            // 没有玩家，取消引爆状态
            this.setSwellDir(-1);
        }
    }

    // **注意：由于爆炸被移到了 aiStep()，tick() 不再需要爆炸检查，但必须保留 super.tick()**

    /**
     * 辅助方法：检查玩家是否正对着实体 (Look At Me Logic)
     */
    private boolean doesPlayerLookAtMe(Player player) {
        // 玩家的视线向量
        Vec3 playerLookVec = player.getViewVector(1.0F).normalize();

        // 玩家到苦力怕的向量
        Vec3 toCreeperVec = new Vec3(this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ()).normalize();

        // 计算点积 (Dot Product)
        double dotProduct = playerLookVec.dot(toCreeperVec);

        // 设定临界值 0.90
        return dotProduct >= 0.90D;
    }


    private void teleportToPlayer(Player player) {
        // 获取玩家的朝向角 (Yaw)，并转换为弧度
        float yaw = player.getYRot() * ((float)Math.PI / 180F);

        // 计算玩家前方的瞬移目标点
        double offsetX = (double)(-Mth.sin(yaw) * TELEPORT_DISTANCE);
        double offsetZ = (double)(Mth.cos(yaw) * TELEPORT_DISTANCE);

        // 目标位置
        double targetX = player.getX() + offsetX;
        double targetY = player.getY();
        double targetZ = player.getZ() + offsetZ;

        // 执行瞬移
        this.teleportTo(targetX, targetY, targetZ);

        // 瞬移后，让 FlashCreeper 面向玩家
        this.setYRot(player.getYRot());
        this.setXRot(player.getXRot());
    }

    @Override
    public void tick() {
        super.tick();
    }

    protected void explodeCreeper() {

        if (!this.level().isClientSide) {
            RandomSource random = this.level().random;

            // 1. 播放爆炸音效
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.GENERIC_EXPLODE, this.getSoundSource(), 3.0F, 0.5F + random.nextFloat() * 0.5F);

            // 2. 增强粒子效果 (添加 setSwellDir(1) 确保视觉效果在爆炸前一刻出现)
            this.setSwellDir(1);

            if (this.level() instanceof ServerLevel serverLevel) {
                random = this.level().random;


                // 1. 烟花火花 (Firework): 数量增加，速度更快
                serverLevel.sendParticles(ParticleTypes.FIREWORK,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        // 数量从 40 -> 70
                        70,
                        // X/Y/Z 扩散范围保持
                        0.5D, 0.5D, 0.5D,
                        // 速度从 0.5 -> 0.8 (飞得更快)
                        0.8D);

                // 2. 爆裂碎片/暴击粒子 (Crit): 数量增加，扩散更大
                serverLevel.sendParticles(ParticleTypes.CRIT,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        // 数量从 80 -> 150
                        150,
                        // X/Y/Z 扩散范围从 0.8 -> 1.0
                        1.0D, 1.0D, 1.0D,
                        0.8D);

                // 3. 亮光/附魔光芒 (Enchant): 数量增加
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        // 数量从 20 -> 40
                        40,
                        0.5D, 0.5D, 0.5D,
                        0.2D);

                // --- 新增彩色和自定义粒子效果 ---

                // 4. 彩色魔法/药水效果 (Entity Effect): 创建蓝色/紫色/绿色等颜色混合的粒子
                // 这种粒子效果默认会随机染色或使用药水颜色。
                serverLevel.sendParticles(ParticleTypes.ENTITY_EFFECT,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        // 数量 50
                        50,
                        0.8D, 0.8D, 0.8D,
                        0.4D);

                // 金色/橙色的闪光
                serverLevel.sendParticles(ParticleTypes.LAVA,
                        this.getX(), this.getY() + 0.5D, this.getZ(),
                        // 数量 30
                        30,
                        0.4D, 0.4D, 0.4D,
                        0.3D);
            }

            // 3. 移除实体自身
            this.discard();
        }
    }
}