package com.first.themasses.events.prop;

import com.first.themasses.item.prop.DreamSakuraProp;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = "themasses")
public class DreamSakuraEvents {

    /**
     * 1. 友好生物逻辑：生物不会主动打你，但你打它它会还手
     */
    @SubscribeEvent
    public static void onLivingTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player && isBlessed(player)) {
            LivingEntity entity = event.getEntity();

            // 如果是 Mob（具有 AI 的生物）
            if (entity instanceof Mob mob) {
                // 只有当玩家【不是】最后一次伤害该生物的人时，才取消锁定
                // 这意味着如果玩家攻击了它，它就能正常还手
                if (mob.getLastHurtByMob() != player) {
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * 2. 核心免疫与替死逻辑：使用 AttackEvent 消除红光、声音和后退动画
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isBlessed(player)) return;

        CompoundTag persisted = getPersistedTag(player);
        DamageSource source = event.getSource();
        float amount = event.getAmount();

        // --- A. 替死逻辑 ---
        List<LivingEntity> potentialScapegoats = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(10.0D),
                e -> e != player && e.isAlive());

        if (!potentialScapegoats.isEmpty()) {
            LivingEntity target = potentialScapegoats.stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .get();

            target.hurt(player.damageSources().generic(), amount);

            // 彻底拦截攻击信号，无受击动画
            event.setCanceled(true);
            return;
        }

        // --- B. 10层满级免疫判定 ---
        String typeKey = "DreamCherryResist_" + source.getMsgId();
        if (persisted.getInt(typeKey) >= 10) {
            // 已经彻底适应，无视此次攻击，无受击动画
            event.setCanceled(true);
        }
    }

    /**
     * 3. 进化抗性逻辑：处理 1-9 层的伤害衰减
     */
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || !isBlessed(player)) return;

        // 关键：从持久化标签读取
        CompoundTag persisted = getPersistedTag(player);
        String typeKey = "DreamCherryResist_" + event.getSource().getMsgId();
        int count = persisted.getInt(typeKey);

        if (count >= 10) {
            // 虽然 AttackEvent 已经拦截了，但为了保险这里设为0
            event.setAmount(0);
        } else {
            // 每一层减少 10% 伤害
            float reduction = count * 0.1f;
            event.setAmount(event.getAmount() * (1.0f - reduction));

            // 增加进化层数，存入持久化 NBT，死亡不会清空
            persisted.putInt(typeKey, count + 1);
        }
    }

    // 辅助：获取跨死亡保留的 NBT
    private static CompoundTag getPersistedTag(Player player) {
        CompoundTag forgeData = player.getPersistentData();
        if (!forgeData.contains(Player.PERSISTED_NBT_TAG)) {
            forgeData.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return forgeData.getCompound(Player.PERSISTED_NBT_TAG);
    }
    //物品转化
    // 1. 捕捉原始伤害
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void captureOriginalDamage(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

        // 只要看到原始伤害达标，就立刻打上标记
        if (event.getAmount() >= 1.0E35F) {
            player.getPersistentData().putBoolean("HighDamageLooming", true);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDreamSakuraEvolution(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide) return;

        CompoundTag data = player.getPersistentData();

        if (data.getBoolean("HighDamageLooming")) {
            data.remove("HighDamageLooming");

            float finalAmount = event.getAmount();
            if (player.isAlive() && finalAmount < player.getHealth()) {

                boolean evolved = false;
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack stack = player.getInventory().getItem(i);

                    if (stack.getItem() instanceof DreamSakuraProp || stack.is(net.minecraft.world.item.Items.PINK_PETALS)) {
                        ItemStack newItem = new ItemStack(com.first.themasses.item.ModItems.DREAMSAKURA.get(), 1);
                        stack.shrink(1);
                        if (!player.getInventory().add(newItem)) {
                            player.drop(newItem, false);
                        }

                        evolved = true;
                        break;
                    }
                }

                if (evolved) {
                    player.displayClientMessage(Component.literal("§d小梦樱的祝福赐向了历经困难的人"), false);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                            net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                }
            }
        }
    }

    private static boolean isBlessed(Player player) {
        CompoundTag data = player.getPersistentData();
        return data.contains(Player.PERSISTED_NBT_TAG) &&
                data.getCompound(Player.PERSISTED_NBT_TAG).getBoolean("DreamCherryBlessed");
    }
}