package com.first.themasses.curios.mysterious;

//1.每1h游戏时长，提供2点护甲值、1护甲韧性、2攻击力、5最大生命值 0.1攻击速度 0.01移动速度（摘掉饰品则消失，重新带上时恢复，不同玩家独立计算）（并没有重置)
//2.距离世界出生点越远，属性加成越强（每500格多1%）
//3.带上物品之后每获得一个成就则增加2%的随机属性（摘下来则重置,重新戴上后重新开始计算）
//4.带上饰品之后每遇见一个新的群系则增加1-5点幸运值（随机），50%增加抢夺和时运等级   （摘下来则重置）
//5.佩戴饰品后，每击败一种你从未用带着饰品击败过的生物，则之后对其伤害增加1%百分比伤害，然后再击杀这个生物共100次的话，则之后对其伤害增加10%百分比伤害，然后再击杀这个生物共10000次的话，则之后对其伤害增加100%百分比伤害(三段式，而非成长式)
//6.时长到达3小时，获得夜视，6小时获得抗火，9小时免疫掉落伤害，12小时免疫中毒、饥饿、缓慢、虚弱
//7.技能：极速、再生与永恒
//进行一次随机数1~10的判定
//发动技能后临时将移动速度提升1000%~10000%一段时间
//在此期间，攻击力与攻速得到极大的加成，随机数越大提升幅度越高
//释放此技能会逐渐扣除生命值，同时技能期间无法恢复生命值，记录技能期间累计扣除的生命值总数
//在主动结束此技能后，以极高速度恢复相当于两倍扣除生命值总量的生命值，同时获得相当于恢复生命值总量的10%~100%的伤害吸收
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosCapability;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * 饰品类：坤钟
 */
public class KunZhong extends Item {

    // 使用固定的 UUID 确保属性修改器在更新时是替换而非叠加
    private static final UUID GROWTH_UUID = UUID.fromString("f48e266a-1234-5678-90ab-cdef12345678");
    private static final UUID SKILL_UUID = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");

    public KunZhong(Properties properties) {
        super(properties);
    }

    /**
     * 核心：计算挂载在饰品上的属性修改器
     */
    public Multimap<Attribute, AttributeModifier> getCurioModifiers(SlotContext slotContext, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> modifiers = LinkedHashMultimap.create();
        CompoundTag nbt = stack.getOrCreateTag();

        // --- 修改 1：改为阶梯式增长 (整数除法) ---
        // 只有当 ticks 达到 72000 的倍数时，hours 才会增加 1
        int hours = nbt.getInt("EquippedTicks") / 72000;

        // 移除严格判定，确保属性实例始终挂载，从而响应 /hour 指令 of NBT 变化
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player)) return modifiers;

        // 距离加成
        BlockPos spawn = player.level().getSharedSpawnPos();
        double dist = Math.sqrt(player.distanceToSqr(spawn.getX(), spawn.getY(), spawn.getZ()));
        double distMult = 1.0 + (Math.floor(dist / 500.0) * 0.01);

        // 成就加成
        double advMult = 1.0 + nbt.getFloat("AdvancementBonus");
        double totalMult = distMult * advMult;

        // 注册属性加成 (基于整数 hours)
        // 即使 hours 为 0，也添加修改器，数值为 0.0，这样 NBT 变化时系统会刷新数值
        // 严格遵循原始公式：只有时长增加，属性才会增加；0小时则所有加成为0
        modifiers.put(Attributes.ARMOR, new AttributeModifier(GROWTH_UUID, "KunZhong Armor", (double)hours * 2.0 * totalMult, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(GROWTH_UUID, "KunZhong Toughness", (double)hours * 1.0 * totalMult, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(GROWTH_UUID, "KunZhong Damage", (double)hours * 2.0 * totalMult, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MAX_HEALTH, new AttributeModifier(GROWTH_UUID, "KunZhong Health", (double)hours * 4.0 * totalMult, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.ATTACK_SPEED, new AttributeModifier(GROWTH_UUID, "KunZhong Attack Speed", (double)hours * 0.1 * totalMult, AttributeModifier.Operation.ADDITION));
        modifiers.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(GROWTH_UUID, "KunZhong Move Speed", (double)hours * 0.01 * totalMult, AttributeModifier.Operation.ADDITION));

        if (nbt.contains("ExtraLuck")) {
            modifiers.put(Attributes.LUCK, new AttributeModifier(GROWTH_UUID, "KunZhong Luck", nbt.getFloat("ExtraLuck"), AttributeModifier.Operation.ADDITION));
        }
        return modifiers;
    }

    /**
     * 每 tick 执行的逻辑
     */
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        LivingEntity entity = slotContext.entity();
        if (!(entity instanceof Player player) || player.level().isClientSide) return;

        CompoundTag nbt = stack.getOrCreateTag();
        CompoundTag playerData = player.getPersistentData();
        long gameTime = player.level().getGameTime();

        // 恢复时长逻辑
        if (nbt.getInt("EquippedTicks") == 0 && playerData.contains("KunZhongStoredTicks")) {
            nbt.putInt("EquippedTicks", playerData.getInt("KunZhongStoredTicks"));
        }

        // 每秒逻辑
        if (gameTime % 20 == 0) {
            int ticks = nbt.getInt("EquippedTicks") + 20;
            nbt.putInt("EquippedTicks", ticks);
            playerData.putInt("KunZhongStoredTicks", ticks); // 同步到玩家存档

            this.handleTimeEffects(player, ticks);
            this.handleBiomeDiscovery(player, nbt);

            // --- 冷却计时 (每秒减1) ---
            int cd = nbt.getInt("SkillCD");
            if (cd > 0) {
                nbt.putInt("SkillCD", cd - 1);
            }
        }

        // --- 技能扣血逻辑 (每秒扣除一次，百分比反噬，可扣死) ---
        if (nbt.getBoolean("SkillActive")) {
            player.getPersistentData().putBoolean("KunZhongSkillImmuneHeal", true);

            // --- 10s 技能时长计时 ---
            int timer = nbt.getInt("SkillTimer");
            if (timer > 0) {
                nbt.putInt("SkillTimer", timer - 1);
            } else {
                // 时间到，自动结束
                this.stopSkill(stack, player);
                return; // 结束逻辑后直接返回，防止继续跑下面的扣血
            }

            // 每 20 tick (1秒) 触发一次
            if (gameTime % 20 == 0) {
                int roll = nbt.getInt("SkillRoll");
                if (roll < 1) roll = 1;

                // 计算每秒扣血百分比
                double secondPercent = 0.03 + (roll - 1) * 0.01;
                float drainAmount = (float) (player.getMaxHealth() * secondPercent);
                float healthBefore = player.getHealth();

                // --- 核心修改：执行扣血并立即检查生命状态 ---
                float targetHealth = healthBefore - drainAmount;
                player.setHealth(targetHealth);

                // 如果这次扣血导致玩家死亡
                if (targetHealth <= 0 || !player.isAlive()) {
                    // 立即原地“自毁”技能 NBT，不依赖外部死亡事件
                    nbt.putBoolean("SkillActive", false);
                    nbt.putInt("SkillCD", 0);
                    nbt.putInt("SkillTimer", 0);
                    nbt.putFloat("SkillDrainedHealth", 0f);
                    player.getPersistentData().remove("KunZhongSkillImmuneHeal");

                    // 强制同步一次客户端属性
                    stack.setPopTime(5);
                    return;
                }

                // 只有活下来了才记录累计扣血量
                float actualDrained = healthBefore - Math.max(0, player.getHealth());
                nbt.putFloat("SkillDrainedHealth", nbt.getFloat("SkillDrainedHealth") + actualDrained);

                // 粒子效果
                player.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE, player.getX(), player.getY() + 1, player.getZ(), 0, 0, 0);
            }
        } else {
            player.getPersistentData().remove("KunZhongSkillImmuneHeal");
        }

        // 9小时掉落免疫
        if (nbt.getInt("EquippedTicks") >= 648000) {
            player.fallDistance = 0;
        }
    }

    /**
     * 启动技能
     */
    public void startSkill(ItemStack stack, Player player) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (nbt.getBoolean("SkillActive")) return;

        // --- 小时数检查 (需要 6 小时) ---
        int hours = nbt.getInt("EquippedTicks") / 72000;
        if (hours < 6) {
            player.displayClientMessage(Component.literal("§c契约尚未成熟... "), true);
            return;
        }
        // --- 冷却检查 ---
        if (nbt.getInt("SkillCD") > 0) {
            player.displayClientMessage(Component.literal("§c技能冷却中: " + nbt.getInt("SkillCD") + "s"), true);
            return;
        }

        // 1. 随机数判定 1~10
        int roll = player.getRandom().nextInt(10) + 1;
        nbt.putInt("SkillRoll", roll);
        nbt.putFloat("SkillDrainedHealth", 0f); // 重置累计扣血
        nbt.putInt("SkillTimer", 200);          // 设定10秒时长 (200 ticks)
        nbt.putBoolean("SkillActive", true);

        // 2. 打上无法回血的标记 (用于 Events 类拦截)
        player.getPersistentData().putBoolean("KunZhongSkillImmuneHeal", true);

        // 3. 强制触发 Curios 属性重算
        stack.setPopTime(5);
        player.displayClientMessage(Component.literal("§c§l永恒契约判定 [" + roll + "]！"), true);
    }

    /**
     * 主动结束技能
     */
    public void stopSkill(ItemStack stack, Player player) {
        CompoundTag nbt = stack.getOrCreateTag();
        if (!nbt.getBoolean("SkillActive")) return;

        // 1. 先读取数据
        float drained = nbt.getFloat("SkillDrainedHealth");
        int roll = nbt.getInt("SkillRoll");

        // 2. 关键：先关闭技能标记并移除禁止回血的标记
        nbt.putBoolean("SkillActive", false);
        nbt.putInt("SkillCD", 100);             // CD设为100s
        nbt.remove("SkillTimer");               // 移除计时器
        player.getPersistentData().remove("KunZhongSkillImmuneHeal");
        // 必须在 heal 之前执行，确保最大血量上限先恢复正常
        stack.setPopTime(5);
        // 4. 结算爆发恢复
        float healAmount = drained * 2.0f;
        // 爆发恢复不受 LivingHealEvent 拦截，因为我们已经在上一步删了标记
        player.heal(healAmount);
        // 5. 伤害吸收
        float absorptionPercent = roll / 10.0f;
        float absorptionAmount = healAmount * absorptionPercent;
        if (absorptionAmount > 0) {
            player.setAbsorptionAmount(player.getAbsorptionAmount() + absorptionAmount);
        }

        // 6. 清理临时数据
        nbt.remove("SkillRoll");
        nbt.remove("SkillDrainedHealth");

        player.displayClientMessage(Component.literal("§a§l永恒契约已达成"), true);
    }

    // 辅助方法：手动触动属性刷新
    private void refreshAttributes(Player player) {
        for (Attribute attr : new Attribute[]{Attributes.MOVEMENT_SPEED, Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED}) {
            AttributeInstance inst = player.getAttribute(attr);
            if (inst != null) inst.removeModifier(GROWTH_UUID);
        }
    }

    /**
     * 处理群系探索：发现新群系增加随机幸运值
     */
    private void handleBiomeDiscovery(Player player, CompoundTag nbt) {
        String biome = player.level().getBiome(player.blockPosition()).unwrapKey().get().location().toString();
        ListTag biomes = nbt.getList("VisitedBiomes", Tag.TAG_STRING);

        boolean isNew = true;
        for (int i = 0; i < biomes.size(); i++) {
            if (biomes.getString(i).equals(biome)) { isNew = false; break; }
        }

        if (isNew) {
            biomes.add(net.minecraft.nbt.StringTag.valueOf(biome));
            nbt.put("VisitedBiomes", biomes);
            float newLuck = nbt.getFloat("ExtraLuck") + (new Random().nextInt(5) + 1);
            nbt.putFloat("ExtraLuck", newLuck);
        }
    }

    /**
     * 随时间获得的特殊效果阶段
     */
    private void handleTimeEffects(Player player, int ticks) {
        double hours = ticks / 72000.0;

        // 基础效果
        if (hours >= 3.0) player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 410, 0, false, false, false));
        if (hours >= 6.0) player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 210, 0, false, false, false));

        // 12小时免疫判定
        if (hours >= 12.0) {
            // 写入标记，告知事件处理器拦截 Buff
            player.getPersistentData().putBoolean("KunZhongImmune", true);
        } else {
            // 时长不足或摘下时，移除标记（摘下清理已在 onUnequip 处理）
            if (player.getPersistentData().contains("KunZhongImmune")) {
                player.getPersistentData().remove("KunZhongImmune");
            }
        }
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<ICurio> lazyCurio = LazyOptional.of(() -> new ICurio() {
                @Override
                public ItemStack getStack() {
                    return stack;
                }

                @Override
                public void curioTick(SlotContext slotContext) {
                    KunZhong.this.curioTick(slotContext, stack);
                }

                @Override
                public void onEquip(SlotContext slotContext, ItemStack prevStack) {
                    LivingEntity entity = slotContext.entity();
                    if (entity instanceof Player player && !player.level().isClientSide) {
                        // 这里的 stack 是正在佩戴的，它是真实的
                        CompoundTag nbt = stack.getOrCreateTag();

                        if (!nbt.contains("KunZhongInit")) {
                            CompoundTag playerData = player.getPersistentData();

                            // 物理清空一下当前的 tag，防止有脏数据残留
                            for (String key : nbt.getAllKeys().stream().toList()) {
                                nbt.remove(key);
                            }

                            // 重新同步时长
                            if (playerData.contains("KunZhongStoredTicks")) {
                                nbt.putInt("EquippedTicks", playerData.getInt("KunZhongStoredTicks"));
                            }

                            // 标记初始化
                            nbt.putBoolean("KunZhongInit", true);

                            // 强制刷新玩家属性
                            for (Attribute attr : new Attribute[]{Attributes.ARMOR, Attributes.MAX_HEALTH, Attributes.ATTACK_DAMAGE, Attributes.ARMOR_TOUGHNESS, Attributes.ATTACK_SPEED, Attributes.MOVEMENT_SPEED, Attributes.LUCK}) {
                                AttributeInstance inst = player.getAttribute(attr);
                                if (inst != null) inst.removeModifier(GROWTH_UUID);
                            }
                            stack.setPopTime(5);
                        }
                    }
                }

                @Override
                public void onUnequip(SlotContext slotContext, ItemStack newStack) {
                    LivingEntity entity = slotContext.entity();
                    if (entity instanceof Player player && !player.level().isClientSide) {

                        // 1. 清理逻辑
                        java.util.function.Consumer<ItemStack> cleanser = (targetStack) -> {
                            if (!targetStack.isEmpty() && targetStack.getItem() == KunZhong.this) {
                                CompoundTag targetNbt = targetStack.getOrCreateTag();
                                // 只要含有初始化标记，说明这是刚从身上卸下的，
                                if (targetNbt.contains("KunZhongInit")) {
                                    //排空所有数据
                                    for (String key : new java.util.ArrayList<>(targetNbt.getAllKeys())) {
                                        targetNbt.remove(key);
                                    }
                                }
                            }
                        };
                        // 2. 捕捉左键点击：清理鼠标上的物品
                        cleanser.accept(player.containerMenu.getCarried());
                        // 3. 捕捉 Shift 点击或快捷键：清理背包里的物品
                        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                            cleanser.accept(player.getInventory().getItem(i));
                        }
                        // 4. 清理当前正在操作的这个 stack 引用 (它是卸下的那一块)
                        cleanser.accept(stack);
                        // --- 玩家持久化数据重置 ---
                        CompoundTag pd = player.getPersistentData();
                        pd.remove("KunZhongVisitedBiomes");
                        pd.remove("KunZhongKillTracker");
                        // --- 属性强制剥离 ---
                        Attribute[] attributesToReset = {
                                Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
                                Attributes.ATTACK_DAMAGE, Attributes.MAX_HEALTH,
                                Attributes.ATTACK_SPEED, Attributes.MOVEMENT_SPEED,
                                Attributes.LUCK
                        };
                        for (Attribute attr : attributesToReset) {
                            AttributeInstance inst = player.getAttribute(attr);
                            if (inst != null) {
                                inst.removeModifier(GROWTH_UUID);
                            }
                        }
                    }
                }

                @Override
                public Multimap<Attribute, AttributeModifier> getAttributeModifiers(SlotContext slotContext, UUID uuid) {
                    return KunZhong.this.getCurioModifiers(slotContext, stack);
                }

                @Override
                public boolean canSync(SlotContext slotContext) {
                    return true;
                }
            });

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
                return CuriosCapability.ITEM.orEmpty(cap, lazyCurio);
            }
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.themasses.Kun_Zhong.line1").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x00AAAA))));

        // 关键：如果摘下后 NBT 已经被设为 null，这里就不会执行，物品看起来就是刚获得的样子
        if (stack.hasTag() && stack.getTag() != null && stack.getTag().contains("EquippedTicks")) {
            int hours = stack.getTag().getInt("EquippedTicks") / 72000;
            if (hours > 0) {
                tooltip.add(Component.literal("佩戴时长: " + hours + "小时").withStyle(Style.EMPTY.withColor(0xFFAA00)));
            }
        }
    }
}