package com.first.themasses.events;

import com.first.themasses.command.title.ZhilingCommand;
import com.first.themasses.curios.mysterious.Duvacein;
import com.first.themasses.curios.mysterious.Kanghuoyaoshui;
import com.first.themasses.item.ModItems;
import com.first.themasses.effect.ModPotions;
import com.first.themasses.util.PlayerCooldownManager; // 导入冷却管理类

// 💥 修正导入：使用正确的 MobEffectEvent 拦截药水
import net.minecraft.ChatFormatting;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event.Result;

// 新增导入：用于虚空返回
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.server.ServerLifecycleHooks;
import top.theillusivec4.curios.api.CuriosApi;
import net.minecraft.world.effect.MobEffects; // 导入药水效果
import net.minecraft.world.effect.MobEffectInstance; // 导入药水效果实例
import net.minecraft.network.chat.Component; // 导入文本组件

import java.util.List;
import java.util.function.Predicate;
//各种事件
@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static final ResourceLocation DIVINE_FRUIT_PIE_RL = new ResourceLocation("enigmaticdelicacy", "divine_fruit_pie");
    private static final RegistryObject<Item> LAOWANG237_FOOD_REGISTRY = ModItems.LAOWANG237_FOOD;
    private static final RegistryObject<Item> LAOWANG237_CURIO_REGISTRY = ModItems.LAOWANG237;
    private static final RegistryObject<Item> DAZHI_CURIO_REGISTRY = ModItems.DAZHI;
    private static final RegistryObject<Item> LYUEZHUO_CURIO_REGISTRY = ModItems.LYUEZHUO;

    //NBT键名
    private static final String MOD_DATA_TAG = "themasses_data";
    private static final String KILL_COUNT_TAG = "ForbiddenFruitKillCount";
    private static final String KILL_TIMER_TAG = "ForbiddenFruitKillTimer";
    public static final String TAG_NEUTRAL_UNTIL = "TheMasses_NeutralUntil"; // 怪物中立 NBT 键

    private static final ResourceLocation KANGHUOYAOSHUI_RL = new ResourceLocation("themasses", "green_hand_potf");
    private static final Predicate<ItemStack> IS_KANGHUOYAOSHUI = stack ->
            stack.is(ForgeRegistries.ITEMS.getValue(KANGHUOYAOSHUI_RL));


    // --- 1. 玩家击杀生物时的掉落逻辑 (老王饰品) ---
    @SubscribeEvent
    public static void onMobKill(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            Level level = player.level();
            LivingEntity deadEntity = event.getEntity();

            if (level.isClientSide) return;
            // 检查玩家是否佩戴了老王饰品
            if (!isWearingLaowangCurio(player)) return;
            if (player.getRandom().nextFloat() > 0.0237f) {
                return; // 如果随机数大于 0.0237，则不触发掉落，直接结束
            }
            // 掉落物品 1: divine_fruit_pie
            Item divineFruitPieItem = ForgeRegistries.ITEMS.getValue(DIVINE_FRUIT_PIE_RL);

            if (divineFruitPieItem != null) {
                int pieCount = getPlayerItemCount(player, divineFruitPieItem);
                if (pieCount < 64) {
                    deadEntity.spawnAtLocation(new ItemStack(divineFruitPieItem, 1));
                }
            }
        }
    }

    //玩家自己死亡时逻辑 (老王和月灼饰品) ---
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        // 检查死亡实体是否为玩家
        if (event.getEntity() instanceof Player player) {
            Level level = player.level();
            if (level.isClientSide) return;

            // ----------------------------------------------------------------
            // 💥 月酌饰品：被动不死图腾效果 + 怪物中立逻辑
            // ----------------------------------------------------------------
            if (isLyuezhuoWorn(player)) {
                if (PlayerCooldownManager.isTotemCooldownReady(player)) {

                    // 阻止死亡
                    event.setCanceled(true);

                    // 模拟不死图腾效果
                    player.setHealth(5.0F);
                    player.removeAllEffects();
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 900, 2)); // 45s 再生 III
                    player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, 1)); // 5s 吸收 II

                    // 重置冷却时间
                    PlayerCooldownManager.resetTotemCooldown(player);

                    // 给予反馈 (只播放音效)
                    player.level().playSound(
                            null, // 声音播放者 (null 表示不是由特定玩家发出的)
                            player.getX(), player.getY(), player.getZ(), // 播放位置
                            net.minecraft.sounds.SoundEvents.TOTEM_USE, // 不死图腾音效
                            net.minecraft.sounds.SoundSource.PLAYERS, // 修正后的声音来源
                            0.6F, // 音量
                            1.0F  // 音高
                    );

                    // ----------------------------------------------------
                    // 💥 新增：让周围怪物中立 7 秒 (设置 NBT 标记)
                    // ----------------------------------------------------

                    final double NEUTRAL_RADIUS = 32.0D;   // 中立半径 32 格
                    final int NEUTRAL_DURATION_TICKS = 7 * 20;  // 7 秒 = 140 ticks

                    // 计算中立结束的绝对游戏时间
                    long neutralEndTime = level.getGameTime() + NEUTRAL_DURATION_TICKS;

                    AABB searchBox = player.getBoundingBox().inflate(NEUTRAL_RADIUS);

                    // 获取当前目标是该玩家的 Mob
                    List<Mob> nearbyMobs = level.getEntitiesOfClass(
                            Mob.class,
                            searchBox,
                            mob -> mob.getTarget() == player
                    );

                    for (Mob mob : nearbyMobs) {
                        // a. 清除目标：立即停止攻击
                        mob.setTarget(null);

                        // b. 设置 NBT 标记：记录中立结束时间
                        CompoundTag tag = mob.getPersistentData();
                        tag.putLong(TAG_NEUTRAL_UNTIL, neutralEndTime); // 使用新的 NBT 键
                    }

                    // 阻止后续的老王饰品掉落逻辑
                    return;
                }
            }

            // ----------------------------------------------------------------
            // 老王饰品：掉落逻辑 (仅当玩家真正死亡时执行)
            // ----------------------------------------------------------------

            // 检查玩家是否佩戴了老王饰品
            if (!isWearingLaowangCurio(player)) return;

            // 掉落物品 2: laowang237_food (内部 Mod 物品)
            Item laowangFoodItem = LAOWANG237_FOOD_REGISTRY.get();
            if (laowangFoodItem != null) {
                int foodCount = getPlayerItemCount(player, laowangFoodItem);
                if (foodCount < 64) {
                    // 使用 player.drop 方法，确保掉落在玩家尸体周围
                    player.drop(new ItemStack(laowangFoodItem, 1), true);
                }
            }
        }
    }


    // --- 3. 经验获取增幅逻辑 (大智饰品功能) ---
    @SubscribeEvent
    public static void onExperienceChange(PlayerXpEvent.XpChange event) {
        if (event.getAmount() <= 0) return;
        Player player = (Player) event.getEntity();
        // 检查玩家是否佩戴了大智饰品
        if (isWearingDaZhiCurio(player)) {
            // 获取当前的经验变化量
            int originalAmount = event.getAmount();
            // 1.20.1 推荐方案：使用 setAmount() 翻倍经验值
            event.setAmount(originalAmount * 2);
        }
    }
    //大智饰品提高附魔等级
    @SubscribeEvent
    public static void onEnchantmentLevelSet(EnchantmentLevelSetEvent event) {

        int currentPower = event.getOriginalLevel();
        net.minecraft.core.BlockPos pos = event.getPos();

        net.minecraft.world.phys.AABB searchArea = new net.minecraft.world.phys.AABB(
                pos.getX() - 2, pos.getY() - 2, pos.getZ() - 2,
                pos.getX() + 3, pos.getY() + 3, pos.getZ() + 3
        );

        // 搜索附魔台周围的玩家
        List<Player> nearbyPlayers = event.getLevel().getEntitiesOfClass(
                Player.class,
                searchArea,
                (entity) -> true
        );
        // 检查是否有玩家佩戴了饰品
        for (Player player : nearbyPlayers) {
            if (isWearingDaZhiCurio(player)) {
                // 找到玩家后，应用强制 +30 增益
                int newPower = currentPower + 30;
                event.setEnchantLevel(newPower);
                return;
            }
        }
    }


    //抗火药水掉落逻辑 (击杀生物 20% 掉落药水)
    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // 确保击杀者是玩家
        if (event.getSource().getEntity() instanceof Player) {
            Player player = (Player) event.getSource().getEntity();

            if (!player.level().isClientSide) {
                // 检查玩家是否佩戴了康火药水饰品
                if (isWearingKanghuoyaoshuiCurio(player)) {

                    // 检查背包中是否已有 4 瓶或以上
                    if (countSpecificPotionInInventory(player) >= 4) {
                        return; // 提前退出，阻止掉落
                    }

                    if (Math.random() < 0.20) { // 20% 概率检查

                        // 创建带有 BAMBOOZLED_POTION 效果的喷溅药水
                        ItemStack potionStack = PotionUtils.setPotion(
                                new ItemStack(Items.SPLASH_POTION),
                                ModPotions.BAMBOOZLED_POTION.get()
                        );

                        // 创建掉落物实体
                        ItemEntity itemEntity = new ItemEntity(
                                player.level(),
                                event.getEntity().getX(),
                                event.getEntity().getY(),
                                event.getEntity().getZ(),
                                potionStack
                        );

                        event.getDrops().add(itemEntity);
                    }
                }
            }
        }
    }
    //抗火药水饰品受到火焰伤害回血
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (!player.level().isClientSide) {

                // 1. 检查是否为火焰或岩浆伤害 (康火药水)
                if (event.getSource().is(DamageTypes.IN_FIRE) ||
                        event.getSource().is(DamageTypes.ON_FIRE) ||
                        event.getSource().is(DamageTypes.LAVA) ||
                        event.getSource().is(DamageTypes.HOT_FLOOR)) {

                    AttributeInstance swimSpeed = player.getAttribute(ForgeMod.SWIM_SPEED.get());

                    if (swimSpeed != null && swimSpeed.getModifier(Kanghuoyaoshui.FIRE_IMMUNITY_UUID) != null) {

                        // 3. 获取伤害值
                        float damageAmount = event.getAmount();

                        // 4. 执行回血操作 (伤害转化为治疗)
                        player.heal(damageAmount);

                        // 5. 关键：取消原来的伤害事件
                        event.setCanceled(true);

                        // 额外的清理：消除着火视觉效果（如果 curioTick 未能及时消除）
                        if (player.isOnFire()) {
                            player.clearFire();
                        }
                    }
                }
            }
        }
    }

    // 末地虚空返回逻辑 (月酌饰品)
    @SubscribeEvent
    public static void onVoidDamage(LivingDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            // 确保是服务端且玩家佩戴饰品
            if (player.level().isClientSide || !isLyuezhuoWorn(player)) {
                return;
            }

            // 检查伤害源是否为虚空
            if (event.getSource().is(DamageTypes.FELL_OUT_OF_WORLD)) {

                // 检查是否在末地
                if (player.level().dimension() == Level.END) {

                    // 1. 取消伤害（阻止死亡）
                    event.setCanceled(true);

                    // 2. 将玩家传送回出生点 (Spawn Point)
                    if (player instanceof ServerPlayer serverPlayer) {

                        // 获取玩家设定的重生点信息
                        BlockPos spawnPos = serverPlayer.getRespawnPosition();
                        float spawnAngle = serverPlayer.getRespawnAngle();

                        // 获取目标世界（玩家设定的重生世界，默认为 Overworld）
                        net.minecraft.resources.ResourceKey<Level> targetDimKey;
                        java.util.Optional<net.minecraft.resources.ResourceKey<Level>> respawnDimOptional = java.util.Optional.of(serverPlayer.getRespawnDimension());

                        // 使用 isPresent/get 逻辑替换 orElse，解决编译问题
                        if (respawnDimOptional.isPresent()) {
                            targetDimKey = respawnDimOptional.get();
                        } else {
                            targetDimKey = Level.OVERWORLD;
                        }

                        // 获取目标世界
                        ServerLevel targetWorld = serverPlayer.server.getLevel(targetDimKey);

                        if (targetWorld == null) {
                            // 如果玩家设定的重生世界加载失败，则退回到 Overworld 的默认世界
                            targetWorld = serverPlayer.server.overworld();
                        }

                        // 如果没有玩家设定的出生点，使用世界的默认出生点
                        if (spawnPos == null) {
                            spawnPos = targetWorld.getSharedSpawnPos();
                        }

                        // 传送玩家
                        serverPlayer.teleportTo(
                                targetWorld,
                                spawnPos.getX() + 0.5D, // 居中
                                spawnPos.getY() + 1.5D, // 避免卡在方块里
                                spawnPos.getZ() + 0.5D, // 居中
                                spawnAngle,
                                0.0F
                        );
                        player.setHealth(player.getMaxHealth()); // 恢复满血
                    }
                }
            }
        }
    }

    //抗火药水饰品攻击着火
    @SubscribeEvent
    public static void onLivingAttackWithCurio(LivingDamageEvent event) {

        // 1. 检查攻击者是否为玩家
        if (event.getSource().getDirectEntity() instanceof Player player) {

            LivingEntity target = event.getEntity();

            // 3. 检查玩家是否佩戴了抗火药水饰品
            if (isWearingKanghuoyaoshuiCurio(player)) {

                // 4. 应用火焰效果
                target.setSecondsOnFire(4);
            }
        }
    }

    // 末地发光效果 (月酌饰品)
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Player player = event.player;

            // 确保在服务端且玩家佩戴饰品
            if (player.level().isClientSide || !isLyuezhuoWorn(player)) {
                return;
            }

            // 检查是否在末地
            if (player.level().dimension() == Level.END) {
                // 持续获得发光效果 (Duration: 200 ticks = 10秒, Amplifier: 0, 无粒子)
                player.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        200,
                        0,
                        false, // 不显示粒子
                        false  // 不显示图标
                ));
            }
        }
    }

    // 免疫漂浮效果 (月酌饰品)
    @SubscribeEvent
    public static void onPotionApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof Player player) {
            // 确保玩家佩戴饰品
            if (isLyuezhuoWorn(player)) {
                // 检查是否是漂浮效果
                if (event.getEffectInstance().getEffect() == MobEffects.LEVITATION) {
                    // 阻止药水效果被施加
                    event.setResult(Result.DENY);
                }
            }
        }
    }
    //伊甸禁果的逻辑
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient()) return;

        ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers().forEach(serverPlayer -> {
            CompoundTag playerData = serverPlayer.getPersistentData();

            if (playerData.contains(MOD_DATA_TAG)) {
                CompoundTag customTag = playerData.getCompound(MOD_DATA_TAG);
                int count = customTag.getInt(KILL_COUNT_TAG);
                int timer = customTag.getInt(KILL_TIMER_TAG);

                if (count > 0) {
                    if (timer > 0) {
                        customTag.putInt(KILL_TIMER_TAG, timer - 1);
                    } else {
                        // 1. 执行 KILL
                        serverPlayer.kill();

                        // 2. 没抗住的逻辑
                        if (!serverPlayer.isAlive()) {
                            serverPlayer.sendSystemMessage(Component.literal("自由的代价即是如此").withStyle(ChatFormatting.DARK_PURPLE));
                            // 死了直接清空，不给经验
                            customTag.remove(KILL_COUNT_TAG);
                            customTag.remove(KILL_TIMER_TAG);
                        } else {
                            // 3. 抗住了，次数减一
                            int newCount = count - 1;
                            customTag.putInt(KILL_COUNT_TAG, newCount);

                            if (newCount > 0) {
                                customTag.putInt(KILL_TIMER_TAG, 60); // 还没完，下一轮
                            } else {
                                // ==================【重点：抗住全部三波后的奖励】==================
                                if (serverPlayer.isAlive()) {
                                    // A. 发送成功文本
                                    serverPlayer.sendSystemMessage(Component.literal("且善用那自由的意志").withStyle(ChatFormatting.DARK_PURPLE));

                                    // B. 计算经验 (把你之前在物品类写的逻辑搬到这里)
                                    boolean isEnd = serverPlayer.level().dimension() == net.minecraft.world.level.Level.END;
                                    int multiplier = isEnd ? 4 : 2;
                                    // 获取总游玩时间
                                    long totalPlayTimeTicks = serverPlayer.getStats().getValue(net.minecraft.stats.Stats.CUSTOM.get(net.minecraft.stats.Stats.PLAY_TIME));
                                    long totalPlayTimeSeconds = totalPlayTimeTicks / 20L;
                                    int experienceAmount = (int) (totalPlayTimeSeconds * multiplier);

                                    // C. 发放经验
                                    if (experienceAmount > 0) {
                                        serverPlayer.giveExperiencePoints(experienceAmount);
                                        serverPlayer.sendSystemMessage(Component.translatable("item.themasses.forbidden_fruit.exp_gained", experienceAmount).withStyle(ChatFormatting.DARK_PURPLE));
                                    }
                                }
                                // ================================================================

                                customTag.remove(KILL_COUNT_TAG);
                                customTag.remove(KILL_TIMER_TAG);
                            }
                        }
                    }
                    // 写回 NBT
                    if (playerData.contains(MOD_DATA_TAG)) {
                        if (customTag.isEmpty()) playerData.remove(MOD_DATA_TAG);
                        else playerData.put(MOD_DATA_TAG, customTag);
                    }
                }
            }
        });
    }


    //进游戏时清空换皮的nbt
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().contains("HuanpiName")) {
                player.getPersistentData().remove("HuanpiName");
                // 进服时发一个空包给客户端，让客户端也清空缓存
                com.first.themasses.networking.ModMessages.INSTANCE.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new com.first.themasses.networking.PacketSyncHuanpi("", "", "")
                );
                System.out.println("DEBUG: 已重置进服玩家的变身状态。");
            }
        }
    }

    //摘饰品时候的检测
    @SubscribeEvent
    public static void onPlayerTick(LivingEvent.LivingTickEvent event) {
        // 1. 服务端运行，且降低频率（每 0.5 秒检查一次，减轻服务器压力）
        if (event.getEntity().level().isClientSide || event.getEntity().tickCount % 10 != 0) return;

        if (event.getEntity() instanceof ServerPlayer player) {
            // 检查 NBT 中是否标记了变身状态
            if (player.getPersistentData().contains("HuanpiName")) {

                // 核心检测：利用 Curios API 检查饰品是否还在槽位里
                boolean isEquipped = top.theillusivec4.curios.api.CuriosApi.getCuriosHelper()
                        .findFirstCurio(player, stack -> stack.getItem() instanceof Duvacein) // 请确保 Duvacein 路径正确
                        .isPresent();

                if (!isEquipped) {
                    System.out.println("DEBUG: 检测到饰品已脱下，正在执行强制恢复逻辑...");

                    // 1. 清除 NBT 数据，防止逻辑循环触发
                    player.getPersistentData().remove("HuanpiName");

                    // 2. 还原服务端 GameProfile 属性（让其他玩家看你也变回来）
                    try {
                        // 彻底移除当前的变身纹理
                        player.getGameProfile().getProperties().removeAll("textures");

                        // 重新抓取一次原始皮肤（带签名的），确保服务端属性是干净的原始数据
                        // 这样别人看你时，不会因为缓存导致你还是变身后的样子
                        player.getServer().getSessionService().fillProfileProperties(player.getGameProfile(), true);
                    } catch (Exception e) {
                        System.err.println("DEBUG: 服务端属性还原异常: " + e.getMessage());
                    }

                    // 3. 发送“空名包”给客户端（触发你自己看自己的 originalSkin 还原逻辑）
                    // 这里的 PacketSyncHuanpi 构造函数会把 null 转为 ""
                    com.first.themasses.networking.ModMessages.INSTANCE.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new com.first.themasses.networking.PacketSyncHuanpi("", "", "")
                    );

                    // 4. 刷新视觉（广播 ADD_PLAYER 包，同步 GameProfile 的修改）
                    // 确保你 TouxianCommand 里的 refresh 方法使用了 EnumSet.allOf(Action.class)
                    ZhilingCommand.refresh(player);
                }
            }
        }
    }

    //小碎石掉落逻辑
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        LevelAccessor levelAccessor = event.getLevel();
        // 确保在服务端运行且能转换为 Level
        if (levelAccessor.isClientSide() || !(levelAccessor instanceof Level level)) return;

        BlockState state = event.getState();
        Player player = event.getPlayer();
        ItemStack tool = player.getMainHandItem();

        // 1. 判断是否是石头类方块
        if (state.is(BlockTags.STONE_ORE_REPLACEABLES) || state.is(Blocks.STONE)) {

            // 2. 优化：如果拥有精准采集，则不掉落小碎石
            if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
                return;
            }

            // 3. 概率计算：基础 1%，受幸运值加成
            double luck = player.getAttributeValue(Attributes.LUCK);
            double dropChance = 0.01 + (luck * 0.002);

            if (player.getRandom().nextDouble() < dropChance) {
                BlockPos pos = event.getPos();
                // 弹出掉落物
                Block.popResource(level, pos, new ItemStack(ModItems.PEBBLE.get()));
            }
        }
    }

    // ====================================================================
    // --- 辅助方法 ---
    // ====================================================================

    /**
     * 辅助方法：检查玩家是否穿戴了 lyuezhuo 饰品。
     * 💥 访问权限已修改为 public static，供 Mixin 访问
     * @param player 目标玩家
     */
    public static boolean isLyuezhuoWorn(Player player) { // 💥 从 private 改为 public
        // 确保 LYUEZHUO_CURIO_REGISTRY 已正确初始化
        Item lyuezhuoItem = LYUEZHUO_CURIO_REGISTRY.get();
        if (lyuezhuoItem == null) return false;

        // 使用 CuriosApi 检查饰品栏
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(stack -> stack.getItem() == lyuezhuoItem).isPresent())
                .orElse(false);
    }


    // 辅助方法：检查玩家是否佩戴了老王饰品
    private static boolean isWearingLaowangCurio(Player player) {
        Item laowangCurioItem = LAOWANG237_CURIO_REGISTRY.get();
        if (laowangCurioItem == null) return false;

        // 使用 CuriosApi 检查饰品栏
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(stack -> stack.getItem() == laowangCurioItem).isPresent())
                .orElse(false);
    }

    // 辅助方法：检查玩家是否佩戴了大智饰品
    private static boolean isWearingDaZhiCurio(Player player) {
        Item dazhiCurioItem = DAZHI_CURIO_REGISTRY.get();
        if (dazhiCurioItem == null) return false;

        // 使用 CuriosApi 检查饰品栏
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(stack -> stack.getItem() == dazhiCurioItem).isPresent())
                .orElse(false);
    }

    // 辅助方法：检查玩家是否佩戴了康火药水饰品
    private static boolean isWearingKanghuoyaoshuiCurio(Player player) {
        // 使用 CuriosApi 检查饰品栏
        return CuriosApi.getCuriosInventory(player)
                .map(inv -> inv.findFirstCurio(IS_KANGHUOYAOSHUI).isPresent())
                .orElse(false);
    }


    // 辅助方法：计算玩家背包中某个物品的总数量
    private static int getPlayerItemCount(Player player, Item item) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }


    // 辅助方法：清点背包中特定药水的数量
    private static int countSpecificPotionInInventory(Player player) {
        int count = 0;

        // 创建用于比较的目标药水 ItemStack
        ItemStack targetPotion = PotionUtils.setPotion(
                new ItemStack(Items.SPLASH_POTION),
                ModPotions.BAMBOOZLED_POTION.get()
        );

        // 遍历玩家的主背包 (包括快捷栏)
        for (ItemStack stack : player.getInventory().items) {
            // 使用 isSameItemSameTags 比较物品类型和 NBT (药水效果)
            if (ItemStack.isSameItemSameTags(stack, targetPotion)) {
                count += stack.getCount();
            }
        }

        // 遍历副手
        for (ItemStack stack : player.getInventory().offhand) {
            if (ItemStack.isSameItemSameTags(stack, targetPotion)) {
                count += stack.getCount();
            }
        }

        return count;
    }
}