package com.first.themasses.Recipes.prop;

import com.first.themasses.item.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class MikuPropDiscoveryEvent {

    // 标记键名，确保唯一性
    private static final String GIVENPROP_TAG = "themasses_mikuprop_given";
    // 目标唱片的注册名
    private static final ResourceLocation TARGET_DISC = new ResourceLocation("cataclysm", "music_disc_the_cataclysmfarer");

    @SubscribeEvent
    public static void onPlayerLieDown(PlayerSleepInBedEvent event) {
        Player player = event.getEntity();
        // 逻辑只在服务端运行，防止发两次
        if (player.level().isClientSide) return;
        // 1. 检查是否已经领取过
        CompoundTag persistentData = player.getPersistentData();
        if (persistentData.getBoolean(GIVENPROP_TAG)) return;

        // 2. 检查玩家背包里是否有指定的唱片
        if (hasTargetDisc(player)) {
            // 3. 发放 mikuprop 物品
            ItemStack reward = new ItemStack(ModItems.MIKUPROP.get());

            if (!reward.isEmpty()) {
                // 如果背包满了，会掉落在地上
                if (!player.getInventory().add(reward)) {
                    player.drop(reward, false);
                }

                // 4. 打上永久标签，防止再次获得
                persistentData.putBoolean(GIVENPROP_TAG, true);

                // 5. 仪式感：播放声音并发送一条提示（可选）
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
    }

    /**
     * 遍历玩家背包查找是否有特定唱片
     */
    private static boolean hasTargetDisc(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (registryName != null && registryName.equals(TARGET_DISC)) {
                return true;
            }
        }
        return false;
    }
}