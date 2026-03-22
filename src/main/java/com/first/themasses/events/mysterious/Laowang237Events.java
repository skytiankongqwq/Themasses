package com.first.themasses.events.mysterious;

import com.first.themasses.TheMasses;
import com.first.themasses.curios.mysterious.laowang237;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.List;

@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class Laowang237Events {

    private static MobEffect ASTRAL_DRUNKENNESS = null;
    private static boolean SEARCHED_FOR_EFFECT = false;

    // --- 1. 拦截夜视获得逻辑 ---
    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();

        // 检查是否正在尝试添加夜视
        if (event.getEffectInstance().getEffect() == MobEffects.NIGHT_VISION && entity instanceof Player player) {

            // 检查玩家是否戴了饰品
            CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof laowang237).ifPresent(slot -> {
                MobEffect astralEffect = getAstralEffect();
                if (astralEffect != null) {
                    MobEffectInstance current = player.getEffect(astralEffect);
                    // 如果身上有该 Buff 且等级为 1 (Amplifier 为 0)
                    if (current != null && current.getAmplifier() == 0) {
                        // 核心操作：直接让该效果不可应用（拦截获得）
                        event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
                    }
                }
            });
        }
    }

    // --- 2. 常驻 Buff 与 村民逻辑 ---
    @SubscribeEvent
    public static void onTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player player)) return;

        CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof laowang237).ifPresent(slot -> {

            // 每秒处理一次村民
            if (player.tickCount % 20 == 0) {
                applyVillagerLogic(player);
            }

            // --- 优化后的 Buff 补充逻辑 ---
            if (player.tickCount % 20 == 5) {
                MobEffect astralEffect = getAstralEffect();
                if (astralEffect != null) {
                    MobEffectInstance current = player.getEffect(astralEffect);

                    // 判定条件：
                    // 1. 玩家完全没有这个效果
                    // 2. 或者 玩家身上只有 1 级效果 (Amplifier == 0) 且持续时间快到了 (小于 10 秒/200 ticks)
                    if (current == null || (current.getAmplifier() == 0 && current.getDuration() < 200)) {
                        // 补充 1 级效果，持续 20 秒
                        player.addEffect(new MobEffectInstance(astralEffect, 400, 0, false, false, true));
                    }

                    // 如果玩家身上已经是 2 级 (Amplifier >= 1)，逻辑会跳过，
                    // 这样你手动喝药水或通过其他方式提升的等级就不会被这个 1 级常驻给“挡住”或“重置”。
                }
            }
        });
    }

    // 获取动态效果（不产生硬前置）
    private static MobEffect getAstralEffect() {
        if (!SEARCHED_FOR_EFFECT) {
            ASTRAL_DRUNKENNESS = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation("enigmaticdelicacy", "astral_drunkenness"));
            SEARCHED_FOR_EFFECT = true;
        }
        return ASTRAL_DRUNKENNESS;
    }

    private static void applyVillagerLogic(Player player) {
        List<Villager> villagers = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(12.0D));
        for (Villager villager : villagers) {
            for (MerchantOffer offer : villager.getOffers()) {
                offer.resetUses();
                try {
                    ObfuscationReflectionHelper.setPrivateValue(MerchantOffer.class, offer, 99999, "maxUses");
                } catch (Exception ignored) {}

                int baseCount = offer.getBaseCostA().getCount();
                if (baseCount > 1) {
                    int targetCount = Math.max(1, (int) Math.ceil(baseCount * 0.8));
                    offer.setSpecialPriceDiff(targetCount - baseCount);
                } else {
                    offer.setSpecialPriceDiff(0);
                }
            }
        }
    }
}