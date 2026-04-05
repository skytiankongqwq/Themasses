package com.first.themasses.events;

import com.first.themasses.curios.mysterious.haozi;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

import static com.first.themasses.item.ModItems.LAOWANG237_FOOD;

@Mod.EventBusSubscriber(modid = "themasses")
public class HaoziEvents {

    // 固定的 UUID，用于属性修改器，防止重复叠加
    private static final UUID HAOZI_ATTR_UUID = UUID.fromString("6a12b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c");

    /**
     * 1. 属性静默注入逻辑 (代替 getAttributeModifiers)
     */
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {
            // 检查玩家是否佩戴了 haozi 饰品
            boolean hasHaozi = CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof haozi).isPresent();

            // --- A. 原版属性 ---
            handleAttr(player, Attributes.LUCK, 30.0, AttributeModifier.Operation.ADDITION, hasHaozi);
            handleAttr(player, Attributes.ATTACK_DAMAGE, 15.0, AttributeModifier.Operation.ADDITION, hasHaozi);
            handleAttr(player, Attributes.ATTACK_SPEED, 1.5, AttributeModifier.Operation.ADDITION, hasHaozi);
            handleAttr(player, Attributes.MOVEMENT_SPEED, 0.05, AttributeModifier.Operation.ADDITION, hasHaozi);

            // --- B. 铁魔法属性 (安全注入) ---
            if (ModList.get().isLoaded("irons_spellbooks")) {
                handleModdedAttr(player, "irons_spellbooks:max_mana", 200.0, AttributeModifier.Operation.ADDITION, hasHaozi);
                handleModdedAttr(player, "irons_spellbooks:spell_power", 0.25, AttributeModifier.Operation.MULTIPLY_BASE, hasHaozi);
                handleModdedAttr(player, "irons_spellbooks:spell_cooldown", -0.25, AttributeModifier.Operation.MULTIPLY_BASE, hasHaozi);
            }
        }
    }

    /**
     * 2. 战斗逻辑 (减伤 & 附加伤害)
     */
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        // 减伤逻辑：受到伤害 -25%
        if (event.getEntity() instanceof Player player) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof haozi).ifPresent(c -> {
                event.setAmount(event.getAmount() * 0.75F);
            });
        }

        // 攻击附加：造成伤害 +3 闪电伤害
        if (event.getSource().getEntity() instanceof Player player) {
            CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof haozi).ifPresent(c -> {
                event.setAmount(event.getAmount() + 3.0F);
                if (player.level() instanceof ServerLevel sl) {
                    LivingEntity victim = event.getEntity();
                    sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, victim.getX(), victim.getY() + 1, victim.getZ(), 5, 0.2, 0.2, 0.2, 0.1);
                }
            });
        }
    }

    /**
     * 3. 生物屏蔽逻辑
     */
    /**
     * 3. 改进型生物屏蔽逻辑：不主动攻击，但受击必反击
     */
    @SubscribeEvent
    public static void onMobTarget(LivingChangeTargetEvent event) {
        if (event.getNewTarget() instanceof Player player) {
            // 检查玩家是否佩戴了 haozi
            CuriosApi.getCuriosHelper().findFirstCurio(player, s -> s.getItem() instanceof haozi).ifPresent(c -> {
                LivingEntity attacker = event.getEntity();
                String typeName = attacker.getType().getDescriptionId();

                // 目标生物名单
                if (typeName.contains("enderman") || typeName.contains("bee") ||
                        typeName.contains("mosquito") || typeName.contains("pixie")) {

                    // --- 核心逻辑：检查仇恨原因 ---
                    // getLastHurtByMob() 会返回最后攻击该生物的实体
                    // 如果最后打它的人不是这个玩家，说明是“主动挑衅”，我们取消攻击
                    if (attacker.getLastHurtByMob() != player) {
                        event.setCanceled(true);

                        // 可选：刷一点粒子效果，表示生物被“安抚”了
                        if (attacker.level() instanceof ServerLevel sl) {
                            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                                    attacker.getX(), attacker.getEyeY(), attacker.getZ(),
                                    3, 0.1, 0.1, 0.1, 0.05);
                        }
                    }
                    // 如果 attacker.getLastHurtByMob() == player，代码会跳过取消，生物会正常反击
                }
            });
        }
    }

    /**
     * 4. 掉落物逻辑 (击杀猪掉落老王食物)
     */
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.animal.Pig pig) {
            if (event.getSource().getEntity() instanceof Player player) {
                CuriosApi.getCuriosHelper().findFirstCurio(player, stack ->
                        stack.getItem() instanceof haozi).ifPresent(slot -> {

                    ItemStack extraDrop = new ItemStack(LAOWANG237_FOOD.get());
                    net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                            pig.level(), pig.getX(), pig.getY(), pig.getZ(), extraDrop
                    );

                    event.getDrops().add(itemEntity);

                    if (pig.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                                pig.getX(), pig.getY() + 0.5, pig.getZ(),
                                10, 0.3, 0.3, 0.3, 0.1);
                    }
                });
            }
        }
    }

    // --- 内部辅助工具方法 ---

    private static void handleAttr(Player player, Attribute attr, double amount, AttributeModifier.Operation op, boolean add) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;

        if (add) {
            if (inst.getModifier(HAOZI_ATTR_UUID) == null) {
                inst.addTransientModifier(new AttributeModifier(HAOZI_ATTR_UUID, "haozi_bonus", amount, op));
            }
        } else {
            if (inst.getModifier(HAOZI_ATTR_UUID) != null) {
                inst.removeModifier(HAOZI_ATTR_UUID);
            }
        }
    }

    private static void handleModdedAttr(Player player, String registryName, double amount, AttributeModifier.Operation op, boolean add) {
        Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation(registryName));
        if (attr != null) {
            handleAttr(player, attr, amount, op, add);
        }
    }
}