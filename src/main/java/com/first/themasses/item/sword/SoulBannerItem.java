package com.first.themasses.item.sword;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

//万魂幡
public class SoulBannerItem extends Item {
    private static final String SOULS = "StoredSouls";
    private static final String KILL_COUNT = "TotalKills";
    private static final String BONUS_ATTACK = "BonusAttack";
    private static final String MAIN_SOUL = "MainSoul";
    private static final String PERC_ATTACK = "AttackBonus";
    private static final String PERC_ARMOR = "ArmorBonus";

    private static final UUID PERC_ATTACK_UUID = UUID.fromString("77627777-6666-5555-4444-333333333333");
    private static final UUID PERC_ARMOR_UUID = UUID.fromString("8888a888-7777-6666-5555-444444444444");

    public SoulBannerItem(Properties pProperties) {
        super(pProperties.stacksTo(1).durability(100000));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!target.isAlive() && attacker instanceof Player player) {
            CompoundTag nbt = stack.getOrCreateTag();
            Level level = player.level();

            // --- 粒子效果逻辑 (仅服务端运行) ---
            if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
                // 产生灵魂火粒子和龙息紫色粒子
                serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, target.getX(), target.getY() + 1.0, target.getZ(), 15, 0.2, 0.5, 0.2, 0.05);
                serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
            }

            // 1. 基础击杀成长
            int totalKills = nbt.getInt(KILL_COUNT) + 1;
            nbt.putInt(KILL_COUNT, totalKills);
            if (totalKills % 10 == 0) {
                nbt.putInt(BONUS_ATTACK, nbt.getInt(BONUS_ATTACK) + 1);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5F, 1.5F);
            }

            // 2. 主魂判定 (杀龙)
            if (target.getType() == EntityType.ENDER_DRAGON && nbt.getInt(MAIN_SOUL) < 1) {
                nbt.putInt(MAIN_SOUL, 1);
                player.sendSystemMessage(Component.literal("已攫取龙魂作为主魂！").withStyle(ChatFormatting.LIGHT_PURPLE));
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 1.0F, 1.0F);
            }

            // 3. 灵魂逻辑 (无限记录) + 幽灵尖叫音效
            boolean isUndead = target.getMobType() == MobType.UNDEAD;
            boolean isPig = target instanceof Pig;
            if (isUndead || isPig) {
                int amount = (isPig && target.hasCustomName() && "laowang237".equals(target.getCustomName().getString())) ? 2 : 1;
                int currentSouls = nbt.getInt(SOULS);
                int newSouls = currentSouls + amount;
                nbt.putInt(SOULS, newSouls);

                // 播放吸魂声
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.GHAST_WARN, SoundSource.PLAYERS, 0.4F, 2.0F);

                // 每100灵魂随机加 1% 属性 + 打铁音效
                if (newSouls / 100 > currentSouls / 100) {
                    if (player.getRandom().nextBoolean()) {
                        nbt.putDouble(PERC_ATTACK, nbt.getDouble(PERC_ATTACK) + 0.01);
                        player.sendSystemMessage(Component.literal("万魂幡锋芒更甚").withStyle(ChatFormatting.DARK_PURPLE));
                    } else {
                        nbt.putDouble(PERC_ARMOR, nbt.getDouble(PERC_ARMOR) + 0.01);
                        player.sendSystemMessage(Component.literal("幽魂缠绕").withStyle(ChatFormatting.DARK_PURPLE));
                    }
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 0.5F, 1.2F);
                }
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public @NotNull Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot == EquipmentSlot.MAINHAND) {
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
            CompoundTag nbt = stack.getOrCreateTag();

            // 基础 30 攻击 (1.0 基础 + 29.0 修改器)
            int bonus = nbt.getInt(BONUS_ATTACK);
            builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", 29.0 + bonus, AttributeModifier.Operation.ADDITION));

            // 百分比攻击力
            double percAtk = nbt.getDouble(PERC_ATTACK);
            if (percAtk > 0) {
                builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(PERC_ATTACK_UUID, "Soul bonus attack", percAtk, AttributeModifier.Operation.MULTIPLY_BASE));
            }

            // 百分比护甲
            double percArmor = nbt.getDouble(PERC_ARMOR);
            if (percArmor > 0) {
                builder.put(Attributes.ARMOR, new AttributeModifier(PERC_ARMOR_UUID, "Soul bonus armor", percArmor * 10, AttributeModifier.Operation.ADDITION));
            }

            // 攻速 1.6
            builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", -2.4, AttributeModifier.Operation.ADDITION));

            return builder.build();
        }
        return super.getAttributeModifiers(slot, stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean isSelected) {
        if (entity instanceof Player player && !level.isClientSide) {
            CompoundTag nbt = stack.getOrCreateTag();
            // 修正：1.20.1 中灵魂满 10000 或拥有主魂可飞行
            boolean canFly = nbt.getInt(MAIN_SOUL) >= 1 || nbt.getInt(SOULS) >= 10000;

            if (isSelected && canFly) {
                if (!player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = true;
                    player.onUpdateAbilities();
                }
            } else if (!player.isCreative() && !player.isSpectator()) {
                if (player.getAbilities().mayfly) {
                    player.getAbilities().mayfly = false;
                    player.getAbilities().flying = false;
                    player.onUpdateAbilities();
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag nbt = stack.getOrCreateTag();
        tooltip.add(Component.translatable("item.themasses.soul_banner.desc").withStyle(ChatFormatting.DARK_PURPLE));

        boolean hasMainSoul = nbt.getInt(MAIN_SOUL) >= 1;
        tooltip.add(Component.literal("主魂: " + (hasMainSoul ? "龙魂" : "缺失")).withStyle(hasMainSoul ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.RED));
        tooltip.add(Component.literal("灵魂总数: " + nbt.getInt(SOULS)).withStyle(ChatFormatting.GOLD));

        if (nbt.getDouble(PERC_ATTACK) > 0) {
            tooltip.add(Component.literal("灵魂共鸣(攻): +" + (int)(nbt.getDouble(PERC_ATTACK)*100) + "%").withStyle(ChatFormatting.BLUE));
        }
    }
}