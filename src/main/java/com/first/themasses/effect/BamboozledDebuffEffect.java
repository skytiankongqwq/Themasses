package com.first.themasses.effect;

import com.first.themasses.networking.ModMessages;
import com.first.themasses.networking.ViewFlipPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LightningBolt;
import com.first.themasses.entity.ModEntities;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
//被坑了药水效果
public class BamboozledDebuffEffect extends MobEffect {

    private static final int SWAP_SLOT_COUNT = 5; // 交换数量

    public BamboozledDebuffEffect(MobEffectCategory pCategory, int pColor) {
        super(pCategory, pColor);

        //增加幸运值
        this.addAttributeModifier(
                Attributes.LUCK,
                "7e43372c-29b0-4613-8687-0b19f6a5e12f",
                20.0,
                AttributeModifier.Operation.ADDITION
        );
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        //2秒触发一次
        int i = 40 >> pAmplifier;
        if (i <= 0) {
            i = 1;
        }
        return pDuration % i == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity pLivingEntity, int pAmplifier) {
        Level level = pLivingEntity.level();

        if (!level.isClientSide && level instanceof ServerLevel serverLevel && pLivingEntity instanceof ServerPlayer serverPlayer) {

            //闪电 0.1%
            boolean triggerLightning = serverLevel.random.nextInt(1000) < 1;

            //背包物品交换 30%
            boolean triggerInventorySwap = serverLevel.random.nextInt(100) < 30;

            //视角翻转 30%
            boolean triggerViewFlip = serverLevel.random.nextInt(100) < 30;

            //苦力怕 50%
            boolean triggerCreeper = serverLevel.random.nextInt(100) < 50;

            if (triggerLightning) {

                serverPlayer.hurt(serverPlayer.damageSources().generic(), Float.MAX_VALUE);
                serverPlayer.sendSystemMessage(Component.literal("你运气真的很好你知道吗..."));

                LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(serverLevel);
                if (lightning != null) {
                    lightning.moveTo(serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ());
                    serverLevel.addFreshEntity(lightning);
                }
            }

            else if (triggerInventorySwap) {
                swapRandomInventorySlots(serverPlayer, SWAP_SLOT_COUNT);
            }

            else if (triggerViewFlip) {
                ModMessages.sendToPlayer(new ViewFlipPacket(), serverPlayer);
            }

            else if (triggerCreeper) {

                EntityType<?> flashCreeperType = ModEntities.FLASH_CREEPER.get();
                LivingEntity flashCreeper = (LivingEntity) flashCreeperType.create(serverLevel);

                if (flashCreeper != null) {

                    double radius = 2.0 + serverLevel.random.nextDouble() * 4.0;
                    double angle = serverLevel.random.nextDouble() * Mth.TWO_PI;

                    double newX = pLivingEntity.getX() + radius * Mth.cos((float) angle);
                    double newZ = pLivingEntity.getZ() + radius * Mth.sin((float) angle);
                    double newY = pLivingEntity.getY();

                    flashCreeper.moveTo(newX, newY, newZ, pLivingEntity.getYRot(), pLivingEntity.getXRot());
                    serverLevel.addFreshEntity(flashCreeper);
                }
            }
        }
    }

    private void swapRandomInventorySlots(ServerPlayer player, int count) {
        Inventory inventory = player.getInventory();

        int maxSlot = Inventory.INVENTORY_SIZE;

        for (int i = 0; i < count; i++) {
            int slot1 = player.serverLevel().random.nextInt(maxSlot);
            int slot2 = player.serverLevel().random.nextInt(maxSlot);

            if (slot1 == slot2) {
                i--;
                continue;
            }

            ItemStack stack1 = inventory.getItem(slot1);
            ItemStack stack2 = inventory.getItem(slot2);

            inventory.setItem(slot1, stack2);
            inventory.setItem(slot2, stack1);
        }
        player.containerMenu.broadcastChanges();
    }
}