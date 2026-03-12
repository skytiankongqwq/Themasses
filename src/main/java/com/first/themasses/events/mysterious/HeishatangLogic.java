package com.first.themasses.events.mysterious;

import com.first.themasses.TheMasses;
import com.first.themasses.curios.mysterious.heishatang;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LootingLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
//黑砂糖击杀掉落
@Mod.EventBusSubscriber(modid = TheMasses.MOD_ID)
public class HeishatangLogic {

    @SubscribeEvent
    public static void onLootingLevel(LootingLevelEvent event) {
        if (event.getDamageSource() != null && event.getDamageSource().getEntity() instanceof Player player) {
            if (CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).isPresent()) {
                event.setLootingLevel(event.getLootingLevel() + 1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof heishatang).ifPresent(slot -> {
            ItemStack curio = slot.stack();
            CompoundTag nbt = curio.getOrCreateTag();
            Random random = new Random();
            LivingEntity victim = event.getEntity();

            // 1. Boss 判定逻辑
            if (!victim.canChangeDimensions()) {
                nbt.putInt("BossKills", nbt.getInt("BossKills") + 1);
            }

            int bossKills = nbt.getInt("BossKills");
            double x = victim.getX(), y = victim.getY(), z = victim.getZ();

            // 2. 核心掉落池逻辑 (30% 概率)
            if (random.nextFloat() < 0.30f) {
                List<ItemStack> pool = new ArrayList<>();
                int count;

                if (bossKills >= 50) {
                    // 阶梯 2：矿石块
                    pool.add(new ItemStack(Items.COAL_BLOCK));
                    pool.add(new ItemStack(Items.IRON_BLOCK));
                    pool.add(new ItemStack(Items.COPPER_BLOCK));
                    pool.add(new ItemStack(Items.GOLD_BLOCK));
                    pool.add(new ItemStack(Items.LAPIS_BLOCK));
                    pool.add(new ItemStack(Items.REDSTONE_BLOCK));
                    pool.add(new ItemStack(Items.DIAMOND_BLOCK));
                    pool.add(new ItemStack(Items.EMERALD_BLOCK));
                    if (bossKills >= 3) pool.add(new ItemStack(Items.NETHERITE_BLOCK));

                    // 矿石块掉落数量：1-2个
                    count = 1 + random.nextInt(2);
                } else {
                    // 阶梯 1：矿石锭
                    pool.add(new ItemStack(Items.COAL));
                    pool.add(new ItemStack(Items.IRON_INGOT));
                    pool.add(new ItemStack(Items.COPPER_INGOT));
                    pool.add(new ItemStack(Items.GOLD_INGOT));
                    pool.add(new ItemStack(Items.LAPIS_LAZULI));
                    pool.add(new ItemStack(Items.REDSTONE));
                    pool.add(new ItemStack(Items.DIAMOND));
                    if (bossKills >= 3) pool.add(new ItemStack(Items.NETHERITE_INGOT));

                    // 矿石锭掉落数量：1-4个
                    count = 1 + random.nextInt(4);
                }

                if (!pool.isEmpty()) {
                    ItemStack drop = pool.get(random.nextInt(pool.size())).copy();
                    drop.setCount(count); // 设置随机后的数量
                    event.getDrops().add(new ItemEntity(player.level(), x, y, z, drop));
                }
            }

            // 3. 独立绿宝石掉落 (20% 概率)
            if (random.nextFloat() < 0.35f) {
                // 随机掉落 1-10 个绿宝石
                int emeraldCount = 1 + random.nextInt(10);
                ItemStack emeraldStack = new ItemStack(Items.EMERALD, emeraldCount);

                event.getDrops().add(new ItemEntity(player.level(), x, y, z, emeraldStack));
            }
        });
    }
}