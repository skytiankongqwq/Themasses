package com.first.themasses.curios.mysterious;

import com.first.themasses.curios.IronsCompat;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;
import java.util.UUID;

import static com.first.themasses.item.ModItems.HAOZI;
//+30幸运
//+15点伤害
//+1.5攻击速度
//-25%受到的伤害
//+0.5速度
//如果有铁魔法：
//+25%法术强度
//-25%法术冷却
//+200法力值
//注入法术：连锁闪电，等级15
//末影人蜜蜂绯红蚊子小精灵什么的将无视你
//攻击将附加3点闪电伤害
//主动技是把9*9范围内的所有生物吸一起

public class haozi extends Item implements ICurioItem {

    private static final UUID LUCK_UUID = UUID.fromString("6a12b3c4-d5e6-4f7a-8b9c-0d1e2f3a4b5c");
    private static final UUID DAMAGE_UUID = UUID.fromString("7b23c4d5-e6f7-4a8b-9c0d-1e2f3a4b5c6d");
    private static final UUID SPEED_UUID = UUID.fromString("8c34d5e6-f7a8-4b9c-0d1e-2f3a4b5c6d7e");

    public haozi(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    @Override
    public void onEquip(SlotContext slotContext, ItemStack prevStack, ItemStack stack) {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsCompat.onHaoziEquip(slotContext, stack);
        }
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            IronsCompat.imbueHaoziSpell(stack);
        }
        return super.initCapabilities(stack, nbt);
    }

    @Override
    public void curioTick(SlotContext slotContext, ItemStack stack) {
        if (ModList.get().isLoaded("irons_spellbooks") && slotContext.entity().tickCount % 40 == 0) {
            IronsCompat.imbueHaoziSpell(stack);
        }
    }

    public static void executeHaoziSkill(ServerPlayer player) {
        net.minecraft.world.item.Item haoziItem = com.first.themasses.item.ModItems.HAOZI.get();

        // 1. 检查冷却 (保持 18s = 360 ticks)
        if (player.getCooldowns().isOnCooldown(haoziItem)) {
            float percent = player.getCooldowns().getCooldownPercent(haoziItem, 0);
            int secondsLeft = Math.round(percent * 18);
            player.displayClientMessage(Component.literal("§c技能冷却中！剩余 " + secondsLeft + " 秒"), true);
            return;
        }

        // 2. 范围 (9x5x9)
        AABB area = player.getBoundingBox().inflate(9D, 2.0D, 9D);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, area, e -> e != player);

        if (!targets.isEmpty()) {
            Vec3 playerPos = player.position();

            for (LivingEntity target : targets) {
                Vec3 targetPos = target.position();
                double distance = playerPos.distanceTo(targetPos);

                if (distance < 1.5D) {
                    // 如果距离已经很近了，直接“瞬移”到玩家位置，防止因速度过快冲过头
                    target.teleportTo(playerPos.x, playerPos.y, playerPos.z);
                } else {
                    // 计算指向玩家的向量，力度根据距离动态调整
                    // 距离越远，力度越大，但最高限制在 3.0，防止实体飞出地图
                    double speed = Math.min(distance * 0.5, 3.0);
                    Vec3 pull = playerPos.subtract(targetPos).normalize().scale(speed);

                    target.setDeltaMovement(pull);
                }

                // 关键：重置掉落伤害，防止生物被吸过来时摔死
                target.fallDistance = 0;
                target.hurtMarked = true;
            }

            // 3. 视觉与反馈
            player.displayClientMessage(Component.literal("§b万象天引！").withStyle(ChatFormatting.BOLD), true);
            player.getCooldowns().addCooldown(haoziItem, 360); // 18秒冷却
        } else {
            player.displayClientMessage(Component.literal("§7范围内没有可吸引的目标"), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.themasses.haozi.line1").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}