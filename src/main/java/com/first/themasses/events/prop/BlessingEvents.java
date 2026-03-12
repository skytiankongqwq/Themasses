package com.first.themasses.events.prop;

import com.first.themasses.TheMasses;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;
//终之祈逻辑
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class BlessingEvents {

    // --- 属性修改器唯一标识符 (UUID) ---
    private static final UUID SPEED_ID = UUID.fromString("f7a1b2c3-d4e5-4f6a-8b9c-0d1e2f3a4b5c");
    private static final UUID STOP_MOVE_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID KB_ID = UUID.fromString("66666666-7777-8888-9999-000000000000");
    private static final UUID REACH_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID SWIM_ID = UUID.fromString("cccccccc-dddd-eeee-ffff-000000000000");

    // 伤害处理锁，防止递归溢出
    private static final String DAMAGE_LOCK = "P7_ProcessingDamage";

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.player.level().isClientSide) return;
        Player player = event.player;

        handleEffectTimers(player);

        // --- 核心属性获取 ---
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        AttributeInstance kbResist = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        AttributeInstance swimSpeed = player.getAttribute(ForgeMod.SWIM_SPEED.get());
        AttributeInstance reach = player.getAttribute(ForgeMod.ENTITY_REACH.get());

        // 1. 普通：迅捷 (+30% 移动速度)
        if (player.getPersistentData().getInt("P7_Common") == 7) {
            if (moveSpeed != null && moveSpeed.getModifier(SPEED_ID) == null) {
                moveSpeed.addTransientModifier(new AttributeModifier(SPEED_ID, "Swiftness", 0.3, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // 2. 普通：水行 (游泳速度提升 50%)
        if (player.getPersistentData().getInt("P7_Common") == 4) {
            if (swimSpeed != null && swimSpeed.getModifier(SWIM_ID) == null) {
                swimSpeed.addTransientModifier(new AttributeModifier(SWIM_ID, "Water Walk", 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }

        // 3. 强化：金刚 (完全免疫击退)
        if (player.getPersistentData().getInt("P7_Epic") == 2) {
            if (kbResist != null && kbResist.getModifier(KB_ID) == null) {
                kbResist.addTransientModifier(new AttributeModifier(KB_ID, "Vajra", 1.0, AttributeModifier.Operation.ADDITION));
            }
        }

        // 4. 神话：绝对防御 (静止状态)
        if (player.getPersistentData().getInt("P7_Myth") == 3) {
            if (moveSpeed != null && moveSpeed.getModifier(STOP_MOVE_ID) == null) {
                moveSpeed.addTransientModifier(new AttributeModifier(STOP_MOVE_ID, "Stop Move", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            if (kbResist != null && kbResist.getModifier(KB_ID) == null) {
                kbResist.addTransientModifier(new AttributeModifier(KB_ID, "Defend KB", 1.0, AttributeModifier.Operation.ADDITION));
            }
            player.setDeltaMovement(0, 0, 0);
        }

        // 5. 扭曲：空间偏移 (提升攻击距离，但随机瞬移)
        if (player.getPersistentData().getInt("P7_Distorted") == 2) {
            if (reach != null && reach.getModifier(REACH_ID) == null) {
                reach.addTransientModifier(new AttributeModifier(REACH_ID, "Warp Reach", 30.0, AttributeModifier.Operation.ADDITION));
            }
            if (player.tickCount % 20 == 0 && player.getRandom().nextFloat() < 0.3f) {
                double ox = (player.getRandom().nextDouble() - 0.5) * 4.0;
                double oz = (player.getRandom().nextDouble() - 0.5) * 4.0;
                player.teleportTo(player.getX() + ox, player.getY(), player.getZ() + oz);
            }
        }

        // 6. 扭曲：血肉流失 (高攻速，高自损)
        if (player.getPersistentData().getInt("P7_Distorted") == 1) {
            if (attackSpeed != null && attackSpeed.getModifier(SPEED_ID) == null) {
                attackSpeed.addTransientModifier(new AttributeModifier(SPEED_ID, "Flesh Speed", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
            if (player.tickCount % 20 == 0) {
                float timeFactor = (float) Math.floor(player.tickCount / 200.0);
                float dmg = (player.getMaxHealth() * (0.03f + (timeFactor * 0.01f))) + 2.0f;
                player.setHealth(player.getHealth() - dmg);
            }
        }

        // 7. 诅咒：生命献祭 (保底 5 点叠加)
        if (player.getPersistentData().getInt("P7_Curse") == 1 && player.tickCount % 20 == 0) {
            float totalDmg = (player.getMaxHealth() * 0.02f) + 5.0f;
            player.hurt(player.level().damageSources().onFire(), totalDmg);
        }
        // 8. 强化：隐身伏击 (赋予隐身)
        if (player.getPersistentData().getInt("P7_Epic") == 5) {
            if (!player.hasEffect(MobEffects.INVISIBILITY)) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0, false, false));
            }
        }

        // [终之祈] 空挥检测逻辑
        if (player.getPersistentData().getBoolean("P7_Final_Active")) {
            if (player.swingTime == 1) {
                player.getPersistentData().putInt("P7_Final_M", 2);
            }
            int missTimer = player.getPersistentData().getInt("P7_Final_M");
            if (missTimer > 0) {
                if (missTimer == 1) {
                    executeFinalCost(player);
                    applyMultiDamage(player, player, 1.0E8f); // 反噬
                    player.getPersistentData().remove("P7_Final_Active");
                }
                player.getPersistentData().putInt("P7_Final_M", missTimer - 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (player.getPersistentData().getBoolean("P7_Final_Active")) {
            executeFinalCost(player);
            // 彻底抹除方块
            player.level().setBlockAndUpdate(event.getPos(), Blocks.AIR.defaultBlockState());
            player.getPersistentData().remove("P7_Final_Active");
            player.getPersistentData().remove("P7_Final_MissCheck");
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.getPersistentData().getBoolean("P7_SlashProcessing")) return;
            if (player.getPersistentData().getBoolean(DAMAGE_LOCK)) return;
            player.getPersistentData().putBoolean(DAMAGE_LOCK, true);

            try {
                LivingEntity target = event.getEntity();
                float amount = event.getAmount();

                // --- 终之祈：全伤害类结算 ---
                if (player.getPersistentData().getBoolean("P7_Final_Active")) {
                    executeFinalCost(player);
                    applyMultiDamage(player, target, 1.0E8f);
                    player.getPersistentData().remove("P7_Final_Active");
                    player.getPersistentData().remove("P7_Final_MissCheck");
                    event.setCanceled(true);
                    return;
                }

                // 1. 普通：千斩 (60% 追加)
                if (player.getPersistentData().getInt("P7_Common") == 1 && player.getRandom().nextFloat() < 0.6f) {
                    player.getPersistentData().putBoolean("P7_SlashProcessing", true);
                    target.invulnerableTime = 0;
                    target.hurt(player.level().damageSources().playerAttack(player), amount * 0.8f);
                    player.getPersistentData().remove("P7_SlashProcessing");
                }

                float mod = 1.0f;
                // 2. 普通：破甲
                if (player.getPersistentData().getInt("P7_Common") == 3) mod *= 1.3f;
                // 3. 普通：精准
                if (player.getPersistentData().getInt("P7_Common") == 5) {
                    int c = player.getPersistentData().getInt("P7_CritCount");
                    if (c < 3) { mod *= 1.5f; player.getPersistentData().putInt("P7_CritCount", c + 1); }
                }
                // 4. 强化：破军 (溅射逻辑)
                if (player.getPersistentData().getInt("P7_Epic") == 3) {
                    List<LivingEntity> list = target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(3.0));
                    for (LivingEntity e : list) {
                        if (e != player && e != target) e.hurt(player.level().damageSources().playerAttack(player), amount * 0.5f);
                    }
                }
                // 5. 强化：无双
                if (player.getPersistentData().getInt("P7_Epic") == 4) {
                    if (player.level().getEntitiesOfClass(Player.class, player.getBoundingBox().inflate(8.0)).size() <= 1) mod *= 1.6f;
                }
                // 6. 神话：死亡宣告
                if (player.getPersistentData().getInt("P7_Myth") == 2) {
                    if (!player.getPersistentData().contains("P7_DeclaredTarget")) {
                        player.getPersistentData().putUUID("P7_DeclaredTarget", target.getUUID());
                    }
                    mod *= 6.0f;
                }
                // 7. 扭曲：血肉流失
                if (player.getPersistentData().getInt("P7_Distorted") == 1) {
                    player.heal(amount * 0.3f);
                    player.invulnerableTime = 0;
                    player.hurt(player.level().damageSources().magic(), 2.0f);
                }
                // 8. 扭曲：被诅咒者
                if (player.getPersistentData().getInt("P7_Distorted") == 3) mod *= 10.0f;
                // 9. 诅咒：刃之回响 (反伤)
                if (player.getPersistentData().getInt("P7_Curse") == 3) {
                    player.invulnerableTime = 0;
                    player.hurt(player.level().damageSources().thorns(player), amount * 1.2f);
                }
                //10.隐身
                if (player.getPersistentData().getInt("P7_Epic") == 5) {
                    mod *= 2.0f;
                    player.removeEffect(MobEffects.INVISIBILITY);
                    clearAllBlessingData(player); // 破隐一击后通常消耗掉该次祈福
                }
                event.setAmount(amount * mod);
            } finally {
                player.getPersistentData().remove(DAMAGE_LOCK);
            }
        }
        // 被攻击者逻辑
        if (event.getEntity() instanceof Player victim) {
            if (victim.getPersistentData().getInt("P7_Myth") == 3) event.setCanceled(true);
            if (victim.getPersistentData().getInt("P7_Distorted") == 3) event.setAmount(Float.MAX_VALUE);
            if (victim.getPersistentData().getInt("P7_Curse") == 2) {
                float s = victim.getPersistentData().getFloat("P7_FragileStack");
                event.setAmount(event.getAmount() + s);
                victim.getPersistentData().putFloat("P7_FragileStack", event.getAmount());
            }
        }
    }

    private static void executeFinalCost(Player player) {
        // 瞬间清空生命(残余1%)与饥饿
        player.setHealth(Math.max(0.5f, player.getHealth() * 0.01f));
        player.getFoodData().setFoodLevel(0);
        player.getFoodData().setSaturation(0);
    }

    private static void applyMultiDamage(Player src, LivingEntity target, float d) {
        // 1. 基础状态破除
        target.invulnerableTime = 0;
        target.setInvulnerable(false);
        // 2. 获取 DamageSources 构造器
        net.minecraft.world.damagesource.DamageSources sources = src.level().damageSources();
        // 3. 遍历注册表中的所有伤害类型
        src.level().registryAccess().registry(net.minecraft.core.registries.Registries.DAMAGE_TYPE).ifPresent(registry -> {
            for (net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> key : registry.registryKeySet()) {
                try {
                    // 每次循环强行重置无敌帧，确保每一发都能打进去
                    target.invulnerableTime = 0;
                    // 核心：使用 Holder 包装伤害类型，构建完整的 DamageSource
                    net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType> holder = registry.getHolderOrThrow(key);
                    net.minecraft.world.damagesource.DamageSource source = new net.minecraft.world.damagesource.DamageSource(holder, src);
                    // 这种调用方式会触发 target.hurt，继而触发所有 Mod 挂载的 LivingHurtEvent
                    target.hurt(source, d);
                    // 如果目标已经彻底死亡（即便在某些复活 Mod 下），强制标记死亡
                    if (target.isDeadOrDying() && target.getHealth() > 0) target.setHealth(0);
                } catch (Exception ignored) {
                    // 过滤掉某些初始化失败的特殊伤害类型
                }
            }
        });
    }

    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.getPersistentData().getInt("P7_Myth") == 3) event.setCanceled(true);
            if (player.getPersistentData().getInt("P7_Myth") == 2) {
                if (player.getPersistentData().contains("P7_DeclaredTarget")) {
                    UUID id = player.getPersistentData().getUUID("P7_DeclaredTarget");
                    if (!event.getEntity().getUUID().equals(id)) event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.getPersistentData().getInt("P7_Common") == 2) {
                AttributeInstance as = player.getAttribute(Attributes.ATTACK_SPEED);
                if (as != null) {
                    as.removeModifier(SPEED_ID);
                    as.addTransientModifier(new AttributeModifier(SPEED_ID, "Combo", 1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                player.getPersistentData().putInt("P7_Common_Timer", 1200);
            }
            if (player.getPersistentData().getInt("P7_Epic") == 1) player.heal(player.getMaxHealth() * 0.05f);
        }
        if (event.getEntity() instanceof Player player) clearAllBlessingData(player);
    }

    @SubscribeEvent
    public static void onPotionApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player p && p.getPersistentData().getInt("P7_Myth") == 1) {
            if (!event.getEffectInstance().getEffect().isBeneficial()) event.setResult(Event.Result.DENY);
        }
    }

    private static void handleEffectTimers(Player player) {
        String[] keys = {"P7_Common", "P7_Epic", "P7_Myth", "P7_Distorted", "P7_Curse"};
        for (String k : keys) {
            String timer = k + "_Timer";
            if (player.getPersistentData().contains(timer)) {
                int t = player.getPersistentData().getInt(timer);
                if (t > 0) player.getPersistentData().putInt(timer, t - 1);
                else clearAllBlessingData(player);
            }
        }
    }

    public static void clearBlessing(Player player, String type) {
        // 1. 获取并清理属性修改器
        AttributeInstance[] insts = {
                player.getAttribute(Attributes.ATTACK_SPEED),
                player.getAttribute(Attributes.MOVEMENT_SPEED),
                player.getAttribute(Attributes.KNOCKBACK_RESISTANCE),
                player.getAttribute(ForgeMod.SWIM_SPEED.get()),
                player.getAttribute(ForgeMod.ENTITY_REACH.get())
        };

        // 根据类型清理特定 UUID (简单粗暴但有效)
        for (AttributeInstance ai : insts) if (ai != null) {
            if (type.equals("Common") || type.equals("Distorted") || type.equals("All")) ai.removeModifier(SPEED_ID);
            if (type.equals("Myth") || type.equals("All")) ai.removeModifier(STOP_MOVE_ID);
            if (type.equals("Epic") || type.equals("Myth") || type.equals("All")) ai.removeModifier(KB_ID);
            if (type.equals("Common") || type.equals("All")) ai.removeModifier(SWIM_ID);
            if (type.equals("Distorted") || type.equals("All")) ai.removeModifier(REACH_ID);
        }

        // 2. 批量移除 NBT 标签
        String[] tags = type.equals("All") ?
                new String[]{"P7_Common", "P7_Epic", "P7_Myth", "P7_Distorted", "P7_Curse", "P7_Final_Active", "P7_Final_MissCheck", "P7_SlashProcessing", "P7_FragileStack", "P7_CritCount", "P7_DeclaredTarget", "P7_Common_Timer", "P7_Epic_Timer", "P7_Myth_Timer", DAMAGE_LOCK} :
                new String[]{"P7_" + type, "P7_" + type + "_Timer", "P7_CritCount", "P7_DeclaredTarget", "P7_FragileStack"};

        for (String tag : tags) player.getPersistentData().remove(tag);
        if (type.equals("Epic") || type.equals("All")) player.setInvisible(false);
        if (type.equals("Common") || type.equals("All")) {
            player.removeEffect(MobEffects.NIGHT_VISION); // 清除夜视
        }
        if (type.equals("Epic") || type.equals("All")) {
            player.setInvisible(false);
            player.removeEffect(MobEffects.INVISIBILITY); // 清除隐身伏击的药效
        }
    }

    // 为了兼容老代码调用，保留这个快捷方式
    public static void clearAllBlessingData(Player player) {
        clearBlessing(player, "All");
    }
}
