package com.first.themasses.item.sword;

import com.first.themasses.effect.ModEffects;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import com.first.themasses.entity.VoidPiercerProjectileEntity;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;
import java.util.UUID;

import static com.first.themasses.curios.mysterious.Auviotre.getShield;

public class VoidPiercer extends SwordItem {
    private final Multimap<Attribute, AttributeModifier> defaultModifiers;

    // 修改构造函数，只接收 Properties，其余参数在内部写死
    public VoidPiercer(Properties properties) {
        // 使用 Tiers.NETHERITE (下界合金等级)，伤害设为 0，攻速设为 -2.4F
        super(Tiers.NETHERITE, 0, -2.4F, properties);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        // 15点总伤害 (1.0 基础 + 14.0 修正)
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 14.0D, AttributeModifier.Operation.ADDITION));
        // 1.6 攻击速度 (4.0 基础 - 2.4 修正)
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4D, AttributeModifier.Operation.ADDITION));
        // 攻击距离 +3
        builder.put(ForgeMod.ENTITY_REACH.get(), new AttributeModifier(UUID.fromString("C6AAD2DA-B19F-4A94-9D69-8079C39E9623"), "Range modifier", 3.0D, AttributeModifier.Operation.ADDITION));
        builder.put(ForgeMod.BLOCK_REACH.get(), new AttributeModifier(UUID.fromString("C6AAD2DA-B19F-4A94-9D69-8079C39E9623"), "Range modifier", 3.0D, AttributeModifier.Operation.ADDITION));

        this.defaultModifiers = builder.build();
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        // 赋予它格挡的动作动画（虽然渲染被你接管了，但逻辑上必须是 BLOCK）
        return UseAnim.BLOCK;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        // 设置格挡的最大持续时间（设置一个很大的值，比如 72000）
        return 72000;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        ItemStack itemstack = player.getItemInHand(hand);

        // --- 功能 1：蹲下右键扔出武器投影 ---
        if (player.isShiftKeyDown()) {
            // 检查冷却：如果物品还在冷却中，则不执行逻辑
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.fail(itemstack);
            }
            if (!level.isClientSide) {
                VoidPiercerProjectileEntity projectile = new VoidPiercerProjectileEntity(level, player);
                projectile.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 6.0F, 0.0F);
                level.addFreshEntity(projectile);
                // 耐久消耗
                itemstack.hurtAndBreak(5, player, (p) -> p.broadcastBreakEvent(hand));
                // --- 新增：设置 1s 冷却 (1秒 = 20 ticks) ---
                player.getCooldowns().addCooldown(this, 20);
            }
            return InteractionResultHolder.sidedSuccess(itemstack, level.isClientSide());
        }

        // --- 功能 2：格挡与冷却逻辑 ---

        // 检查是否佩戴了 laowang237 饰品
        boolean hasJewelry = CuriosApi.getCuriosHelper().findFirstCurio(player, stack ->
                stack.getItem().toString().contains("laowang237")
        ).isPresent();

        // 如果没有饰品，则检查并设置冷却
        if (!hasJewelry) {
            // 2.37s * 20t/s = 47.4 -> 取 47 ticks
            player.getCooldowns().addCooldown(this, 47);
        }

        // 开始使用物品（进入格挡状态）
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(itemstack);
    }
    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        // 1. 生成 0.0 到 1.0 之间的随机数，如果小于 0.237 (即 23.7% 概率)
        if (attacker.getRandom().nextFloat() < 0.237F) {
            // 2. 检查目标是否已经拥有 zitanhua Buff
            if (!target.hasEffect(ModEffects.ZITANHUA.get())) {
                // 3. 给予一级（amplifier为0），持续30秒（600 ticks）的Buff
                target.addEffect(new MobEffectInstance(ModEffects.ZITANHUA.get(), 600, 0));
            }
        }
        // 调用父类逻辑处理耐久消耗等
        return super.hurtEnemy(stack, target, attacker);
    }


    // --- 核心：附魔多类型兼容 ---
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        EnchantmentCategory category = enchantment.category;
        // 兼容：剑、刀（通常是剑类）、弓、三叉戟
        return category == EnchantmentCategory.WEAPON ||
                category == EnchantmentCategory.BOW ||
                category == EnchantmentCategory.TRIDENT ||
                category == EnchantmentCategory.BREAKABLE; // 包含耐久等常规
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof Player player && player.isShiftKeyDown()) {
            // 1. 检查冷却
            if (player.getCooldowns().isOnCooldown(this)) return false;

            Level level = player.level();
            if (!level.isClientSide) {
                // --- A. 射线追踪寻找 16 格内的目标 ---
                Vec3 eyePos = player.getEyePosition();
                Vec3 lookVec = player.getViewVector(1.0F);
                Vec3 traceEnd = eyePos.add(lookVec.scale(32.0));

                AABB searchBox = player.getBoundingBox().expandTowards(lookVec.scale(32.0)).inflate(2.0);
                Entity target = null;
                double closestDist = 32.0;

                for (Entity e : level.getEntities(player, searchBox, ent -> ent instanceof LivingEntity && ent.isAlive())) {
                    AABB entityBox = e.getBoundingBox().inflate(e.getPickRadius() + 0.5);
                    java.util.Optional<Vec3> hitPos = entityBox.clip(eyePos, traceEnd);
                    if (hitPos.isPresent()) {
                        double dist = eyePos.distanceTo(hitPos.get());
                        if (dist < closestDist) {
                            closestDist = dist;
                            target = e;
                        }
                    }
                }

                // --- B. 执行瞬移与视角锁定逻辑 ---
                if (target instanceof LivingEntity livingTarget) {
                    // 目标坐标
                    double targetX = livingTarget.getX();
                    double targetY = livingTarget.getY();
                    double targetZ = livingTarget.getZ();

                    // 2. 计算瞬移位置：目标头顶 3.5 格处
                    double teleportX = targetX;
                    double teleportY = targetY + 3.5;
                    double teleportZ = targetZ;

                    // 3. 计算视角旋转 (让玩家看着目标)
                    // 计算从瞬移点到目标中心的向量
                    double dx = targetX - teleportX;
                    double dy = (targetY + livingTarget.getEyeHeight() * 0.5) - (teleportY + player.getEyeHeight());
                    double dz = targetZ - teleportZ;
                    double distanceXZ = Math.sqrt(dx * dx + dz * dz);

                    // 将向量转换为旋转角度 (角度制)
                    float yaw = (float) (Math.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
                    float pitch = (float) (-(Math.atan2(dy, distanceXZ) * (180.0 / Math.PI)));

                    // 4. 执行瞬移并强制同步旋转
                    if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        // 使用支持传入旋转角度的传送方法，这能确保客户端视角瞬间同步
                        serverPlayer.teleportTo(serverPlayer.serverLevel(), teleportX, teleportY, teleportZ, yaw, pitch);
                    } else {
                        player.teleportTo(teleportX, teleportY, teleportZ);
                        player.setYRot(yaw);
                        player.setXRot(pitch);
                    }

                    // 5. 坠机效果：给玩家一个极大的向下速度
                    player.setDeltaMovement(0, -3.0, 0);

                    // 6. 对目标施加下落动量和伤害
                    livingTarget.setDeltaMovement(livingTarget.getDeltaMovement().add(0, -2.0, 0));
                    livingTarget.hurtMarked = true;
                    livingTarget.hurt(player.damageSources().magic(), 12.0F);
                    applyAbyssCorruption(livingTarget);

                    // 7. 演出效果与冷却
                    player.displayClientMessage(Component.literal("§d⚡ 虚空坠击：锁定！"), true);
                    player.getCooldowns().addCooldown(this, 80); // 4秒冷却
                    stack.hurtAndBreak(15, player, (p) -> p.broadcastBreakEvent(player.getUsedItemHand()));
                }
            }
        }
        return super.onEntitySwing(stack, entity);
    }

    private void applyAbyssCorruption(LivingEntity target) {
        // 注意：Enigmatic Legacy 的 ID 通常是 "enigmaticlegacy"
        // 如果你的环境确实是 "enigmaticdelicacy"，请保持原样
        String modId = "enigmaticlegacy";
        if (ModList.get().isLoaded(modId)) {
            MobEffect abyssCorruption = ForgeRegistries.MOB_EFFECTS.getValue(
                    new ResourceLocation(modId, "abyss_corruption")
            );
            if (abyssCorruption != null) {
                // 30秒 = 600 ticks，等级 1
                target.addEffect(new MobEffectInstance(abyssCorruption, 600, 0));
            }
        }
    }


    @Override
    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? this.defaultModifiers : super.getDefaultAttributeModifiers(slot);
    }
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        float shield = getShield(stack);
        tooltip.add(Component.translatable("tooltip.themasses.zitanhua.line1")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9933FF))));
        // 1. 动态生成闪烁的玩家名
        Component dynamicName;
        if (level != null && level.isClientSide) {
            String name = net.minecraft.client.Minecraft.getInstance().player.getScoreboardName();
            // 每 1000 毫秒循环一次，前 500 毫秒乱码，后 500 毫秒正常
            boolean isObfuscated = (System.currentTimeMillis() % 1000) < 500;

            dynamicName = Component.literal(name).withStyle(style ->
                    isObfuscated ? style.withObfuscated(true).withColor(ChatFormatting.DARK_RED)
                            : style.withObfuscated(false).withColor(ChatFormatting.GOLD)
            );
        } else {
            dynamicName = Component.literal("你").withStyle(ChatFormatting.GOLD);
        }
        // 第一条 Line：动态 ID + 深渊低语
        tooltip.add(Component.empty()
                .append(dynamicName)
                .append(Component.literal("，深渊再次为你投下目光。此次不再收取代价，任何……")
                        .withStyle(ChatFormatting.DARK_PURPLE)));
        // 第二条 Line：站队 + 诱导反问
        tooltip.add(Component.literal("这是投资，亦是站队……但祂对你当前百利而无一害，不是吗……")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x9933FF)).withItalic(true)));
        // 调用父类的 appendHoverText 保持链条完整
        super.appendHoverText(stack, level, tooltip, flag);
    }
}