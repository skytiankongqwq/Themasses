package com.first.themasses.events.curios;

import com.first.themasses.curios.all.MarbleShardItem;
import com.first.themasses.item.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.UUID;

import static com.first.themasses.item.ModItems.LAOWANG237_FOOD;
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class MarbleShardEvents {

    // 使用一个固定的 UUID，确保不会重复添加多个加成
    private static final UUID ATTACK_SPEED_MODIFIER_UUID = UUID.fromString("f4702302-3165-4f3b-b27b-99d86a67ed9d");
    private static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
            ATTACK_SPEED_MODIFIER_UUID, "Marble Shard Passive Speed", 1.0, AttributeModifier.Operation.ADDITION);

    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        // 只在服务端处理逻辑，且目标必须是玩家
        if (event.getEntity() instanceof Player player && !player.level().isClientSide) {

            AttributeInstance attackSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
            if (attackSpeed == null) return;

            // 1. 检查玩家是否佩戴了大理石碎片
            boolean hasCurio = CuriosApi.getCuriosHelper().findFirstCurio(player, stack ->
                    stack.getItem() instanceof com.first.themasses.curios.all.MarbleShardItem).isPresent();

            if (hasCurio) {
                // 如果佩戴了，但属性里还没加这个修改器，就加上
                if (!attackSpeed.hasModifier(SPEED_MODIFIER)) {
                    attackSpeed.addTransientModifier(SPEED_MODIFIER);
                }
            } else {
                // 如果没佩戴，但属性里有这个修改器，就移除
                if (attackSpeed.hasModifier(SPEED_MODIFIER)) {
                    attackSpeed.removeModifier(ATTACK_SPEED_MODIFIER_UUID);
                }
            }
        }
    }

    // --- 这里保留你之前的 LivingHurtEvent (闪电伤害逻辑) ---
    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getSource().getEntity() instanceof Player player && !player.level().isClientSide) {

            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof MarbleShardItem).ifPresent(slot -> {
                // 1. 增加 1.5 点闪电伤害
                event.setAmount(event.getAmount() + 3F);

                // 2. 闪电视觉效果
                if (player.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                            event.getEntity().getX(), event.getEntity().getY() + event.getEntity().getBbHeight()/2, event.getEntity().getZ(),
                            5, 0.2, 0.2, 0.2, 0.1);

                    serverLevel.playSound(null, event.getEntity().getX(), event.getEntity().getY(), event.getEntity().getZ(),
                            SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.3F, 2.0F);
                }
            });
        }
    }
}