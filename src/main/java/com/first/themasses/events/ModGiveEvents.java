package com.first.themasses.events;

import com.first.themasses.item.ModItems; // 1. 导入你的 ModItems 类
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;

@SuppressWarnings("removal")

@Mod.EventBusSubscriber(modid = "themasses")
public class ModGiveEvents {

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        Level level = player.level();

        // 1. 确保逻辑只在服务端执行
        if (level.isClientSide()) return;

        // 2. 检查是否为夜晚，并且月相为满月 (Minecraft中 level.getMoonPhase() 返回 0-7，其中 0 代表满月)
        if (!level.isNight() || level.getMoonPhase() != 0) return;

        // 3. 检查玩家是否能看到天空（防止在屋子里卡bug）
        if (!level.canSeeSky(player.blockPosition())) return;

        // 4. 检查玩家是否在抬头射箭 (玩家的俯仰角 pitch 小于 -30 度，说明在朝上方看)
        // Minecraft 中：pitch 往下看为正(最大90)，往上看为负(最小-90)
        if (player.getXRot() > -30.0F) return;

        // ==================== 达成条件，发放奖励 ====================

        // 【修改点】使用 ModItems.LYUEZHUO.get() 获取你自定义的物品
        ItemStack rewardItem = new ItemStack(ModItems.LYUEZHUO.get(), 1);

        // 尝试将物品放入背包，如果背包满则掉落在地上
        if (!player.getInventory().add(rewardItem)) {
            player.drop(rewardItem, false);
        }

        // 在屏幕正下方发送提示信息
        player.displayClientMessage(Component.literal("§d天国注视着你"), true);
    }
    @SubscribeEvent
    public static void APPLEJoin(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        // 1. 确保只在服务端执行
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel serverLevel)) return;

        // 2. 检查生成的实体是否是掉落物 (ItemEntity)
        if (event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();

            // 3. 检查掉落物是否是原版苹果
            if (stack.is(Items.APPLE)) {
                net.minecraft.core.BlockPos pos = itemEntity.blockPosition();

                // 4. 检查是否在坐标 (0,0) 周围的 3*3 区块内 (即区块坐标 x: -1 到 1, z: -1 到 1)
                net.minecraft.world.level.ChunkPos dropChunk = new net.minecraft.world.level.ChunkPos(pos);
                boolean inArea = Math.abs(dropChunk.x) <= 1 && Math.abs(dropChunk.z) <= 1;

                if (inArea) {
                    // 5. 检查掉落位置附近（1格范围内）是否有橡树叶方块，确保它是从树上掉落的
                    boolean nearOakLeaves = false;
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dy = 0; dy <= 1; dy++) {
                            for (int dz = -1; dz <= 1; dz++) {
                                net.minecraft.core.BlockPos checkPos = pos.offset(dx, dy, dz);
                                if (serverLevel.getBlockState(checkPos).is(net.minecraft.world.level.block.Blocks.OAK_LEAVES)) {
                                    nearOakLeaves = true;
                                    break;
                                }
                            }
                            if (nearOakLeaves) break;
                        }
                        if (nearOakLeaves) break;
                    }

                    if (nearOakLeaves) {
                        // 6. 7.77% 概率判定 (0.0777)
                        if (serverLevel.random.nextDouble() < 0.0777) {
                            // 替换为你的禁果物品
                            itemEntity.setItem(new ItemStack(com.first.themasses.item.ModItems.FORBIDDEN_FRUIT_OF_EDEN.get(), stack.getCount()));
                        }
                    }
                }
            }
        }
    }

    // 用于在伤害时记录使徒对应的“最后攻击者实体”
    private static final java.util.Map<java.util.UUID, net.minecraft.world.entity.Entity> APOSTLE_LAST_ATTACKERS = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onLivingDamage(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // 1. 检查受伤的是不是 Goety 使徒 (goety:apostle)
        net.minecraft.resources.ResourceLocation victimId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (victimId == null || !victimId.getNamespace().equals("goety") || !victimId.getPath().equals("apostle")) {
            return;
        }

        // 2. 获取伤害来源的攻击者
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        net.minecraft.world.entity.Entity killer = source.getEntity();
        net.minecraft.world.entity.Entity directKiller = source.getDirectEntity();
        net.minecraft.world.entity.Entity actualAttacker = killer != null ? killer : directKiller;

        if (actualAttacker != null) {
            // 实时记录或覆盖当前的攻击者实体（如果是玩家攻击，这里就会记录为玩家）
            APOSTLE_LAST_ATTACKERS.put(event.getEntity().getUUID(), actualAttacker);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        net.minecraft.world.level.Level level = event.getEntity().level();

        // 1. 检查当前是否在下界
        if (level.dimension() != net.minecraft.world.level.Level.NETHER) return;

        // 2. 检查死去的生物是不是使徒
        net.minecraft.resources.ResourceLocation victimId = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType());
        if (victimId == null || !victimId.getNamespace().equals("goety") || !victimId.getPath().equals("apostle")) {
            return;
        }


        // 3. 取出最后记录的攻击者实体，并从缓存中移除
        net.minecraft.world.entity.Entity lastAttacker = APOSTLE_LAST_ATTACKERS.remove(event.getEntity().getUUID());
        if (lastAttacker == null) {
            return;
        }

        // 4. 严格校验：最后造成伤害的必须是 Goety 体系的仆从（如果最后是玩家或其它生物打的，这里会直接失败）
        if (!isGoetyServant(lastAttacker)) {
            return;
        }

        // 5. 尝试获取该仆从的主人玩家
        net.minecraft.world.entity.player.Player targetPlayer = null;
        if (lastAttacker instanceof net.minecraft.world.entity.OwnableEntity ownable) {
            net.minecraft.world.entity.Entity owner = ownable.getOwner();
            if (owner instanceof net.minecraft.world.entity.player.Player player) {
                targetPlayer = player;
            }
        }

        // 6. 发放奖励到背包
        if (targetPlayer != null) {
            ItemStack rewardItem = new ItemStack(ModItems.WU1WU2.get(), 1);

            if (!targetPlayer.getInventory().add(rewardItem)) {
                targetPlayer.drop(rewardItem, false);
            }

            level.playSound(null, targetPlayer.getX(), targetPlayer.getY(), targetPlayer.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);

            targetPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[芸芸众生]你已获得认可"), true);
        }
    }

    private static boolean isGoetyServant(net.minecraft.world.entity.Entity entity) {
        net.minecraft.resources.ResourceLocation registryName = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        String regNameStr = registryName != null ? registryName.toString().toLowerCase() : "";
        String className = entity.getClass().getName().toLowerCase();

        return regNameStr.contains("goety") || className.contains("goety");
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        // 仅在服务端、且在 Tick 结束时执行，避免重复触发
        if (event.side == LogicalSide.CLIENT || event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }

        net.minecraft.world.entity.player.Player player = event.player;

        // 1. 严格校验：必须在下界维度
        if (player.level().dimension() != net.minecraft.world.level.Level.NETHER) {
            return;
        }

        // 2. 检查玩家当前是否打开了酿造台 GUI
        if (player.containerMenu instanceof net.minecraft.world.inventory.BrewingStandMenu brewingMenu) {

            // 3. 检查前三个槽位是否全都是空玻璃瓶 (Items.GLASS_BOTTLE)
            boolean allBottles = true;
            for (int i = 0; i < 3; i++) {
                ItemStack bottleStack = brewingMenu.getSlot(i).getItem();
                if (!bottleStack.is(net.minecraft.world.item.Items.GLASS_BOTTLE)) {
                    allBottles = false;
                    break;
                }
            }

            if (!allBottles) return;

            // 4. 检查第 4 个槽位（原料槽）放的是不是绿宝石块
            ItemStack ingredientStack = brewingMenu.getSlot(3).getItem();
            if (ingredientStack.is(net.minecraft.world.item.Items.EMERALD_BLOCK)) {

                // 5. 使用 NBT 标签防重复：检查这个绿宝石块是否已经领过奖了
                net.minecraft.nbt.CompoundTag tag = ingredientStack.getOrCreateTag();
                if (!tag.getBoolean("TheMassesRewardClaimed")) {
                    // 标记为已领取，这样只要它一直放在里面，就不会每 Tick 重复触发
                    tag.putBoolean("TheMassesRewardClaimed", true);
                    ingredientStack.setTag(tag);

                    // ==================== 达成条件，瞬间发放奖励 ====================
                    ItemStack rewardItem = new ItemStack(ModItems.GREEN_HAND_POTF.get(), 1);

                    // 放入玩家背包（满了则安全掉落在脚下）
                    if (!player.getInventory().add(rewardItem)) {
                        player.drop(rewardItem, false);
                    }

                    // 播放音效与提示
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.BREWING_STAND_BREW, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);

                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[芸芸众生] 这其实是瓶药水"), true);
                }
            }
        }
    }

    // 16种原版染色玻璃列表
    private static final java.util.List<net.minecraft.world.level.block.Block> STAINED_GLASS_BLOCKS = java.util.List.of(
            net.minecraft.world.level.block.Blocks.WHITE_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.ORANGE_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.MAGENTA_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.LIGHT_BLUE_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.YELLOW_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.LIME_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.PINK_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.GRAY_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.LIGHT_GRAY_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.CYAN_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.PURPLE_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.BLUE_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.BROWN_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.GREEN_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.RED_STAINED_GLASS,
            net.minecraft.world.level.block.Blocks.BLACK_STAINED_GLASS
    );

    @SubscribeEvent
    public static void onBeaconRightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) event.getLevel();
        net.minecraft.core.BlockPos pos = event.getPos();

        // 1. 检查点击的方块是否是信标
        if (serverLevel.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beacon) {

            // 2. 检查信标是否为满级（通过直接扫描下方 4 层金字塔结构，彻底避免映射报错）
            if (!isFullLevelBeacon(serverLevel, pos)) {
                return;
            }

            // 3. 检查信标上方是否放置了全部 16 种不同颜色的玻璃
            if (!hasAll16GlassColors(serverLevel, pos)) {
                return;
            }

            // 4. 使用 BlockEntity 的 PersistentData 防重复领取
            net.minecraft.nbt.CompoundTag tag = beacon.getPersistentData();
            if (tag.getBoolean("TheMassesBeaconRewarded")) {
                return; // 该信标已经触发过奖励了
            }
            tag.putBoolean("TheMassesBeaconRewarded", true);

            // ==================== 达成条件，发放奖励 ====================
            net.minecraft.world.entity.player.Player player = event.getEntity();
            ItemStack rewardItem = new ItemStack(ModItems.XIN_TIRIS.get(), 1);

            if (!player.getInventory().add(rewardItem)) {
                player.drop(rewardItem, false);
            }

            // 播放专属挑战完成音效与升级音效
            serverLevel.playSound(null, pos, net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);

            // 屏幕下方发送提示
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[芸芸众生] 你激活了一只啥猫猫提出的彩色信标"), true);
        }
    }

    // 辅助方法：严谨判定下方是否为完整的 4 层信标金字塔
    private static boolean isFullLevelBeacon(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos beaconPos) {
        // 4级信标需要向下检查 4 层
        for (int i = 1; i <= 4; i++) {
            int y = beaconPos.getY() - i;
            for (int x = beaconPos.getX() - i; x <= beaconPos.getX() + i; x++) {
                for (int z = beaconPos.getZ() - i; z <= beaconPos.getZ() + i; z++) {
                    net.minecraft.core.BlockPos checkPos = new net.minecraft.core.BlockPos(x, y, z);
                    net.minecraft.world.level.block.state.BlockState state = level.getBlockState(checkPos);

                    // 检查是否为合法的信标底座方块（铁、金、钻石、绿宝石、下界合金）
                    if (!isBeaconBaseBlock(state)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean isBeaconBaseBlock(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.IRON_BLOCK) ||
                state.is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK) ||
                state.is(net.minecraft.world.level.block.Blocks.DIAMOND_BLOCK) ||
                state.is(net.minecraft.world.level.block.Blocks.EMERALD_BLOCK) ||
                state.is(net.minecraft.world.level.block.Blocks.NETHERITE_BLOCK);
    }

    // 辅助方法：扫描信标正上方垂直方向上的所有方块，检查是否集齐 16 种颜色的玻璃
    private static boolean hasAll16GlassColors(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos beaconPos) {
        java.util.Set<net.minecraft.world.level.block.Block> foundGlass = new java.util.HashSet<>();

        int startY = beaconPos.getY() + 1;
        int maxHeight = Math.min(level.getMaxBuildHeight(), startY + 64); // 向上扫描最多 64 格

        for (int y = startY; y < maxHeight; y++) {
            net.minecraft.core.BlockPos currentPos = new net.minecraft.core.BlockPos(beaconPos.getX(), y, beaconPos.getZ());
            net.minecraft.world.level.block.Block block = level.getBlockState(currentPos).getBlock();

            if (STAINED_GLASS_BLOCKS.contains(block)) {
                foundGlass.add(block);
            }
        }

        // 必须集齐全部 16 种颜色
        return foundGlass.size() == 16;
    }

    // 记录每个玩家在 1 分钟内命名过的猪 (玩家UUID -> (猪UUID -> 命名时间戳))
    private static final java.util.Map<java.util.UUID, java.util.Map<java.util.UUID, Long>> PLAYER_PIG_NAMING_RECORDS = new java.util.HashMap<>();

    // 记录已经完成过该挑战并获得奖励的玩家，防止重复触发
    private static final java.util.Set<java.util.UUID> COMPLETED_PLAYERS = new java.util.HashSet<>();

    @SubscribeEvent
    public static void onPlayerNamePig(net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        net.minecraft.server.level.ServerLevel serverLevel = (net.minecraft.server.level.ServerLevel) event.getLevel();
        net.minecraft.world.entity.player.Player player = event.getEntity();

        // 如果该玩家已经完成过这个挑战，直接跳过
        if (COMPLETED_PLAYERS.contains(player.getUUID())) {
            return;
        }

        // 1. 检查目标实体是否为原版猪 (minecraft:pig)
        net.minecraft.world.entity.Entity target = event.getTarget();
        if (!(target instanceof net.minecraft.world.entity.animal.Pig)) {
            return;
        }

        // 2. 检查玩家手中拿的是不是命名牌 (Name Tag)
        ItemStack heldItem = event.getItemStack();
        if (!heldItem.is(net.minecraft.world.item.Items.NAME_TAG)) {
            return;
        }

        // 3. 检查命名牌的名字是否严格等于 "player__7"
        String customName = heldItem.getHoverName().getString().trim();
        if (!customName.equals("player__7")) {
            return;
        }

        // 4. 统计与滑动窗口逻辑 (1分钟 = 60000毫秒)
        long currentTime = System.currentTimeMillis();
        java.util.UUID playerId = player.getUUID();
        java.util.UUID pigId = target.getUUID();

        java.util.Map<java.util.UUID, Long> pigRecords = PLAYER_PIG_NAMING_RECORDS.computeIfAbsent(playerId, k -> new java.util.HashMap<>());

        // 记录或更新这只猪被该玩家命名的时间
        pigRecords.put(pigId, currentTime);

        // 清理掉超过 1 分钟（60000ms）的旧记录
        pigRecords.values().removeIf(timestamp -> currentTime - timestamp > 60000L);

        System.out.println("[TheMasses Debug] 玩家 " + player.getName().getString() + " 在 1 分钟内已成功命名 " + pigRecords.size() + " 只不同的猪为 player__7");

        // 5. 检查是否集齐 12 只
        if (pigRecords.size() >= 12) {
            // 标记为已完成
            COMPLETED_PLAYERS.add(playerId);
            // 清空记录
            pigRecords.clear();

            // ==================== 达成条件，发放奖励 ====================
            ItemStack rewardItem = new ItemStack(ModItems.DAZHI.get(), 1);

            if (!player.getInventory().add(rewardItem)) {
                player.drop(rewardItem, false);
            }

            // 播放炫酷的挑战完成音效
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);

            // 屏幕下方发送提示
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[芸芸众生] §b你成功在1分钟内将12只猪命名为 player__7，神秘奖励已发放！"), true);
        }
    }

    @SubscribeEvent
    public static void onhaoziDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        net.minecraft.world.entity.LivingEntity entity = event.getEntity();

        boolean isTargetMatch = false;

        // 1. 判断是否为猪，且自定义名称为 "laowang237"
        if (entity instanceof net.minecraft.world.entity.animal.Pig pig) {
            if (pig.hasCustomName() && pig.getCustomName() != null) {
                if (pig.getCustomName().getString().trim().equals("laowang237")) {
                    isTargetMatch = true;
                }
            }
        }
        // 或者判断是否为玩家，且名字为 "laowang237"
        else if (entity instanceof net.minecraft.world.entity.player.Player player) {
            if (player.getName().getString().trim().equals("laowang237")) {
                isTargetMatch = true;
            }
        }

        if (!isTargetMatch) return;

        // 2. 判定 0.5152% 的概率 (0.5152% = 0.005152)
        // 使用 entity 自带的随机源，保证随机性安全可靠
        if (entity.getRandom().nextDouble() < 1.005152) {
            net.minecraft.world.level.Level level = entity.level();
            net.minecraft.core.BlockPos pos = entity.blockPosition();

            // 3. 创建奖励物品实体并加入掉落列表
            ItemStack rewardItem = new ItemStack(ModItems.HAOZI.get(), 1);
            net.minecraft.world.entity.item.ItemEntity extraDrop = new net.minecraft.world.entity.item.ItemEntity(
                    level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rewardItem
            );

            event.getDrops().add(extraDrop);

        }
    }

    // ---------------------------------------------------------
    // 效果一：击杀 ID 为 wu1wu2 的玩家时，有 50% 概率掉落你自己的 mod 物品 大理石碎片
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerWu1Wu2Drops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // 检查死去的生物是不是玩家，且名字严格为 "wu1wu2"
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            if (player.getName().getString().equals("wu1wu2")) {

                // 50% 概率 (0.5)
                if (player.getRandom().nextDouble() < 0.5) {
                    // 使用你自己的 Mod 物品 WU1WU2
                    ItemStack rewardItem = new ItemStack(com.first.themasses.item.ModItems.MARBLESHARD.get(), 1);

                    net.minecraft.world.level.Level level = player.level();
                    net.minecraft.core.BlockPos pos = player.blockPosition();

                    net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                            level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rewardItem
                    );
                    event.getDrops().add(drop);

                }
            }
        }
    }

    // ---------------------------------------------------------
    // 效果二：挖掘“严重磨损的粉砂质大理石”时，有 2.5% 概率额外掉落你自己的 mod 物品 大理石碎片
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onBlockBreak(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        // 【核心软依赖检查】若没有加载 goety 模组，直接跳过
        if (!net.minecraftforge.fml.ModList.get().isLoaded("goety")) {
            return;
        }

        // 检查被破坏的方块是否为 Goety 的 silt_marble_heavy
        net.minecraft.world.level.block.state.BlockState state = event.getState();
        net.minecraft.resources.ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());

        if (blockId != null && blockId.getNamespace().equals("goety") && blockId.getPath().equals("silt_marble_heavy")) {

            if (event.getLevel().getRandom().nextDouble() < 0.025) {
                // 使用你自己的 Mod 物品 WU1WU2
                ItemStack rewardItem = new ItemStack(com.first.themasses.item.ModItems.MARBLESHARD.get(), 1);

                net.minecraft.world.level.Level level = (net.minecraft.world.level.Level) event.getLevel();
                net.minecraft.core.BlockPos pos = event.getPos();

                // 直接在方块中心生成掉落物实体
                net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                        level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, rewardItem
                );
                level.addFreshEntity(drop);

            }
        }
    }

    // ---------------------------------------------------------
    // 效果：玩家第一次获得“唤魂镰刀 (SummonerScythe)”时，自动附赠另一个物品
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerCheckFirstScythe(TickEvent.PlayerTickEvent event) {
        // 仅在服务端、且每隔 40 Tick (2秒) 执行一次，兼顾实时性与服务器性能
        if (event.side == LogicalSide.CLIENT || event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }

        net.minecraft.world.entity.player.Player player = event.player;
        if (player.tickCount % 40 != 0) {
            return;
        }

        // 1. 检查该玩家是否已经领取过这个“初次获得奖励”
        net.minecraft.nbt.CompoundTag persistentData = player.getPersistentData();
        net.minecraft.nbt.CompoundTag persistentTag;

        if (!persistentData.contains(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            persistentTag = new net.minecraft.nbt.CompoundTag();
            persistentData.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persistentTag);
        } else {
            persistentTag = persistentData.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        }

        // 如果已经触发过了，直接跳过
        if (persistentTag.getBoolean("ReceivedSummonerScytheBonus")) {
            return;
        }

        // 2. 检查玩家背包或主副手中是否拥有唤魂镰刀
        boolean hasScythe = false;

        // 检查主手和副手
        if (player.getMainHandItem().getItem() instanceof com.first.themasses.item.prop.SummonerScythe ||
                player.getOffhandItem().getItem() instanceof com.first.themasses.item.prop.SummonerScythe) {
            hasScythe = true;
        } else {
            // 扫描整个背包
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && stack.getItem() instanceof com.first.themasses.item.prop.SummonerScythe) {
                    hasScythe = true;
                    break;
                }
            }
        }

        // 3. 如果检测到玩家拥有了唤魂镰刀，且是第一次
        if (hasScythe) {
            // 标记为已领取，永久写入玩家数据
            persistentTag.putBoolean("ReceivedSummonerScytheBonus", true);
            persistentData.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persistentTag);

            // ==================== 发放附赠的另一个物品 ====================
            // 这里以发放 LYUEZHUO 为例，你也可以改成你想送的任何其他物品
            ItemStack bonusItem = new ItemStack(ModItems.YANWENZI.get(), 1);

            if (!player.getInventory().add(bonusItem)) {
                player.drop(bonusItem, false); // 背包满了则掉在脚下
            }

            // 视听反馈
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                    net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 0.8F, 1.2F);

            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§d[芸芸众生] §b初次执掌裁决镰刀，得了额外奖励！"), true);
        }
    }

    // ---------------------------------------------------------
    // 效果：将改名为“灵液瓶”的蜂蜜瓶丢在地上，被闪电劈中时转化为新物品（增加防烧毁保护）
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onItemStruckByLightning(net.minecraftforge.event.entity.EntityStruckByLightningEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        // 1. 检查被雷劈中的实体是否为掉落物实体 (ItemEntity)
        if (event.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity itemEntity) {
            ItemStack stack = itemEntity.getItem();

            // 2. 检查掉落物是否为蜂蜜瓶 (Items.HONEY_BOTTLE)
            if (stack.is(net.minecraft.world.item.Items.HONEY_BOTTLE)) {

                // 3. 检查物品是否被赋予了自定义名字，且严格等于 "灵液瓶"
                if (stack.hasCustomHoverName() && stack.getHoverName().getString().trim().equals("灵液瓶")) {

                    net.minecraft.world.level.Level level = itemEntity.level();
                    net.minecraft.core.BlockPos pos = itemEntity.blockPosition();

                    // 【核心修复】将掉落物实体设为无敌状态，防止被闪电生成的火烧毁
                    itemEntity.setInvulnerable(true);

                    // 4. 创建转化后的目标物品（保留原数量）
                    ItemStack transformedStack = new ItemStack(com.first.themasses.item.ModItems.SPIRIT_LIQUID_VIAL.get(), stack.getCount());

                    // 直接替换掉落物实体中的物品
                    itemEntity.setItem(transformedStack);

                    // 5. 炫酷的视听反馈（雷击音效 + 电火花粒子）
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER, net.minecraft.sounds.SoundSource.WEATHER, 0.5F, 2.0F);
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.2F);

                    if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                        serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 30, 0.5, 0.5, 0.5, 0.15);
                    }

                }
            }
        }
    }

    private static final java.util.Map<java.util.UUID, java.util.UUID> PLAYER_CURRENT_BOSS = new java.util.HashMap<>();
    private static final java.util.Map<java.util.UUID, Boolean> BOSS_FIGHT_TOOK_DAMAGE = new java.util.HashMap<>();

    private static boolean isBoss(net.minecraft.world.entity.Entity entity) {
        if (entity == null) return false;
        net.minecraft.resources.ResourceLocation forgeBossTag = new net.minecraft.resources.ResourceLocation("forge", "bosses");
        net.minecraft.resources.ResourceLocation mcBossTag = new net.minecraft.resources.ResourceLocation("minecraft", "bosses");
        net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> forgeKey =
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, forgeBossTag);
        net.minecraft.tags.TagKey<net.minecraft.world.entity.EntityType<?>> mcKey =
                net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE, mcBossTag);
        return entity.getType().is(forgeKey) || entity.getType().is(mcKey);
    }

    // ---------------------------------------------------------
    // 效果：监听伤害事件，从战斗打响的瞬间（无论是谁先动手）开始严格追踪无伤
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onBossHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        net.minecraft.world.entity.LivingEntity target = event.getEntity();

        // 1. 如果受伤的是玩家，且伤害来源是 Boss
        if (target instanceof net.minecraft.world.entity.player.Player player) {
            net.minecraft.world.entity.Entity attacker = event.getSource().getEntity();
            if (isBoss(attacker)) {
                java.util.UUID playerId = player.getUUID();
                java.util.UUID bossId = attacker.getUUID();

                // 绑定当前战斗的 Boss
                PLAYER_CURRENT_BOSS.put(playerId, bossId);
                // 只要玩家挨打，立刻将无伤状态置为 true（代表已受伤，且无法被后续操作覆盖）
                BOSS_FIGHT_TOOK_DAMAGE.put(playerId, true);
            }
        }

        // 2. 如果受伤的是 Boss，且攻击者是玩家
        if (isBoss(target)) {
            if (event.getSource().getEntity() instanceof net.minecraft.world.entity.player.Player player) {
                java.util.UUID playerId = player.getUUID();
                java.util.UUID bossId = target.getUUID();

                // 绑定当前战斗的 Boss
                PLAYER_CURRENT_BOSS.putIfAbsent(playerId, bossId);

                // 【核心修复】使用 putIfAbsent：
                // 如果玩家之前已经被 Boss 打过（tookDamage 已经是 true），绝对不会被这里重置清零！
                // 只有当这是双方第一次交锋且玩家没挨过打时，才初始化为 false
                BOSS_FIGHT_TOOK_DAMAGE.putIfAbsent(playerId, false);
            }
        }
    }

    // ---------------------------------------------------------
    // 效果：监听生物死亡事件，结算无伤击败挑战
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onBossDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) return;

        net.minecraft.world.entity.LivingEntity deadEntity = event.getEntity();

        if (isBoss(deadEntity)) {
            net.minecraft.world.entity.Entity killer = event.getSource().getEntity();

            if (killer instanceof net.minecraft.world.entity.player.Player player) {
                java.util.UUID playerId = player.getUUID();
                java.util.UUID bossId = deadEntity.getUUID();

                if (PLAYER_CURRENT_BOSS.containsKey(playerId) && PLAYER_CURRENT_BOSS.get(playerId).equals(bossId)) {
                    boolean tookDamage = BOSS_FIGHT_TOOK_DAMAGE.getOrDefault(playerId, true);
                    boolean isStoneSword = player.getMainHandItem().is(net.minecraft.world.item.Items.STONE_SWORD);

                    // 结算：全程未受伤 且 最终击杀时手持石剑
                    if (!tookDamage && isStoneSword) {
                            ItemStack rewardItem = new ItemStack(ModItems.PLAYER__7.get(), 1);

                        if (!player.getInventory().add(rewardItem)) {
                            player.drop(rewardItem, false);
                        }

                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    }

                    // 清理缓存
                    PLAYER_CURRENT_BOSS.remove(playerId);
                    BOSS_FIGHT_TOOK_DAMAGE.remove(playerId);
                }
            }
        }
    }

    // 7 个游戏日对应的总 Tick 数 (7 * 24000 = 168000)
    // 【测试提示】测试时请把这里改小（比如改成 100，即 5 秒钟），正式发布时改回 168000
    private static final int REQUIRED_TICKS = 168000;

    @SubscribeEvent
    public static void onAccessoryTick(TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide() || event.phase != TickEvent.Phase.END) {
            return;
        }

        Player player = event.player;

        // 每秒 (20 Tick) 检测一次，兼顾服务器性能
        if (player.tickCount % 20 != 0) {
            return;
        }

        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistentTag;

        if (!persistentData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistentTag = new CompoundTag();
            persistentData.put(Player.PERSISTED_NBT_TAG, persistentTag);
        } else {
            persistentTag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        }

        // 如果已经领过奖了，直接跳过
        if (persistentTag.getBoolean("AccessoryRewardClaimed")) {
            return;
        }

        // 严格检查是否佩戴在 Curios 饰品栏中
        boolean isWearing = isWearingCuriosAccessory(player);

        if (isWearing) {
            int currentTicks = persistentTag.getInt("AccessoryWearTicks");
            currentTicks += 20;
            persistentTag.putInt("AccessoryWearTicks", currentTicks);

            // 达到目标时间，直接在原地发放奖励！
            if (currentTicks >= REQUIRED_TICKS) {
                persistentTag.putBoolean("AccessoryRewardClaimed", true);

                Item rewardItemObj = ModItems.FINAL_PRAYER.get();
                if (rewardItemObj != null) {
                    // 发放终极奖励物品
                    ItemStack rewardItem = new ItemStack(rewardItemObj, 1);
                    if (!player.getInventory().add(rewardItem)) {
                        player.drop(rewardItem, false);
                    }

                    // 视听反馈
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 1.0F, 0.8F);

                }
            }
        } else {
            // 只要摘下来（未佩戴在饰品栏），立刻清空累计时间，从头重算！
            int oldTicks = persistentTag.getInt("AccessoryWearTicks");
            if (oldTicks > 0) {
                persistentTag.putInt("AccessoryWearTicks", 0);
            }
        }
    }

    /**
     * 辅助方法：严格通过 Curios API 检查玩家是否在饰品栏中佩戴了目标物品
     */
    private static boolean isWearingCuriosAccessory(Player player) {
        Item targetAccessory = com.first.themasses.item.ModItems.PLAYER__7.get();
        if (targetAccessory == null) {
            return false;
        }

        try {
            return !CuriosApi.getCuriosHelper()
                    .findCurios(player, targetAccessory)
                    .isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    // ---------------------------------------------------------
    // 效果：玩家背包集齐所有原矿、矿石(锭)、矿石块时触发一次奖励
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void kuangshijiance(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.player.level().isClientSide() || event.phase != net.minecraftforge.event.TickEvent.Phase.END) {
            return;
        }

        net.minecraft.world.entity.player.Player player = event.player;

        // 每秒 (20 Tick) 检测一次背包，兼顾服务器性能
        if (player.tickCount % 20 != 0) {
            return;
        }

        net.minecraft.nbt.CompoundTag persistentData = player.getPersistentData();
        net.minecraft.nbt.CompoundTag persistentTag;

        if (!persistentData.contains(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            persistentTag = new net.minecraft.nbt.CompoundTag();
            persistentData.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persistentTag);
        } else {
            persistentTag = persistentData.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
        }

        // 如果已经获得过该成就奖励，直接跳过
        if (persistentTag.getBoolean("AllOresRewardClaimed")) {
            return;
        }

        // 检查玩家背包是否按你的定义集齐了所有项
        if (hasCollectedAllStrictItems(player)) {
            persistentTag.putBoolean("AllOresRewardClaimed", true);

            // 发放奖励物品 (WU1WU2)
            net.minecraft.world.item.Item rewardItemObj = ModItems.HEISHATANG.get();
            if (rewardItemObj != null) {
                net.minecraft.world.item.ItemStack rewardItem = new net.minecraft.world.item.ItemStack(rewardItemObj, 1);
                if (!player.getInventory().add(rewardItem)) {
                    player.drop(rewardItem, false);
                }

                // 视听反馈
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }
    }

    private static boolean hasCollectedAllStrictItems(net.minecraft.world.entity.player.Player player) {
        // 1. 【原矿】未烧制的矿石方块、粗矿物品、粗矿块
        // 矿石方块（普通/深层/下界/残骸，满足其一即可）
        if (!hasEither(player, net.minecraft.world.item.Items.COAL_ORE, net.minecraft.world.item.Items.DEEPSLATE_COAL_ORE)) return false;
        if (!hasEither(player, net.minecraft.world.item.Items.IRON_ORE, net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE)) return false;
        if (!hasEither(player, net.minecraft.world.item.Items.COPPER_ORE, net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE)) return false;
        if (!hasEither(player, net.minecraft.world.item.Items.GOLD_ORE, net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE)) return false;
        if (!hasEither(player, net.minecraft.world.item.Items.REDSTONE_ORE, net.minecraft.world.item.Items.DEEPSLATE_REDSTONE_ORE)) return false;
        if (!hasEither(player, net.minecraft.world.item.Items.LAPIS_ORE, net.minecraft.world.item.Items.DEEPSLATE_LAPIS_ORE)) return false;
        if (!hasEither(player, net.minecraft.world.item.Items.DIAMOND_ORE, net.minecraft.world.item.Items.DEEPSLATE_DIAMOND_ORE)) return false;
        if (!hasEither(player, net.minecraft.world.item.Items.EMERALD_ORE, net.minecraft.world.item.Items.DEEPSLATE_EMERALD_ORE)) return false;
        if (!hasItemInInventory(player, net.minecraft.world.item.Items.NETHER_GOLD_ORE)) return false;
        if (!hasItemInInventory(player, net.minecraft.world.item.Items.NETHER_QUARTZ_ORE)) return false;
        if (!hasItemInInventory(player, net.minecraft.world.item.Items.ANCIENT_DEBRIS)) return false;

        // 粗矿物品与粗矿块
        net.minecraft.world.item.Item[] rawItemsAndBlocks = {
                net.minecraft.world.item.Items.RAW_IRON,
                net.minecraft.world.item.Items.RAW_IRON_BLOCK,
                net.minecraft.world.item.Items.RAW_COPPER,
                net.minecraft.world.item.Items.RAW_COPPER_BLOCK,
                net.minecraft.world.item.Items.RAW_GOLD,
                net.minecraft.world.item.Items.RAW_GOLD_BLOCK
        };
        for (net.minecraft.world.item.Item item : rawItemsAndBlocks) {
            if (!hasItemInInventory(player, item)) return false;
        }

        // 2. 【矿石】特指各种金属锭 (Iron, Gold, Copper, Netherite)
        net.minecraft.world.item.Item[] ingots = {
                net.minecraft.world.item.Items.IRON_INGOT,
                net.minecraft.world.item.Items.GOLD_INGOT,
                net.minecraft.world.item.Items.COPPER_INGOT,
                net.minecraft.world.item.Items.NETHERITE_INGOT
        };
        for (net.minecraft.world.item.Item item : ingots) {
            if (!hasItemInInventory(player, item)) return false;
        }

        // 3. 【矿石块】由九个锭合成的金属块 (Iron Block, Gold Block, Copper Block, Netherite Block)
        net.minecraft.world.item.Item[] ingotStorageBlocks = {
                net.minecraft.world.item.Items.IRON_BLOCK,
                net.minecraft.world.item.Items.GOLD_BLOCK,
                net.minecraft.world.item.Items.COPPER_BLOCK,
                net.minecraft.world.item.Items.NETHERITE_BLOCK
        };
        for (net.minecraft.world.item.Item item : ingotStorageBlocks) {
            if (!hasItemInInventory(player, item)) return false;
        }

        return true;
    }

    private static boolean hasItemInInventory(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item targetItem) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.is(targetItem)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasEither(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.Item item1, net.minecraft.world.item.Item item2) {
        return hasItemInInventory(player, item1) || hasItemInInventory(player, item2);
    }

    // ---------------------------------------------------------
    // 效果：空手击杀末影龙时，头盔槽位的绑定诅咒南瓜头自动转化成物品
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onEnderDragonDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // 1. 检查死亡的实体是否为末影龙
        if (!(event.getEntity() instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon)) {
            return;
        }

        // 2. 检查伤害来源是否为玩家，且玩家当前为主手空手
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            if (!player.getMainHandItem().isEmpty()) {
                return; // 不是空手击杀，不触发
            }

            // 3. 检查头盔槽位是否有南瓜头 (雕刻过的南瓜)
            net.minecraft.world.item.ItemStack helmet = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
            if (!helmet.isEmpty() && helmet.is(net.minecraft.world.item.Items.CARVED_PUMPKIN)) {

                // 4. 检查该南瓜头是否带有【绑定诅咒】 (Curse of Binding)
                boolean hasBindingCurse = net.minecraft.world.item.enchantment.EnchantmentHelper.getTagEnchantmentLevel(
                        net.minecraft.world.item.enchantment.Enchantments.BINDING_CURSE, helmet) > 0;

                if (hasBindingCurse) {
                    // 强制清空头盔槽（打破绑定诅咒的限制）
                    player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);

                    // 5. 自动转化并给予奖励物品 (WU1WU2)
                    net.minecraft.world.item.Item rewardItemObj = com.first.themasses.item.ModItems.HEISHATANGRING.get();
                    if (rewardItemObj != null) {
                        net.minecraft.world.item.ItemStack rewardItem = new net.minecraft.world.item.ItemStack(rewardItemObj, 1);
                        if (!player.getInventory().add(rewardItem)) {
                            player.drop(rewardItem, false);
                        }

                        // 视听反馈
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------
    // 效果：在夜晚的雪原且高度 >= 200 时击杀敌对生物，达成一次性挑战获得物品
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onHostileDeathAtHighSnowy(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // 1. 检查死亡的实体是否为敌对生物 (Monster)
        if (!(event.getEntity() instanceof net.minecraft.world.entity.monster.Monster)) {
            return;
        }

        // 2. 检查伤害来源是否为玩家
        net.minecraft.world.damagesource.DamageSource source = event.getSource();
        if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {
            net.minecraft.world.level.Level level = player.level();

            // 3. 检查高度是否 >= 200
            if (player.getY() < 200.0D) {
                return;
            }

            // 4. 检查是否为夜晚
            if (!level.isNight()) {
                return;
            }

            // 5. 检查当前生物群系是否为雪原/雪地 (通过原版 BiomeTags 判断)
            net.minecraft.core.BlockPos pos = player.blockPosition();
            boolean isSnowy = level.getBiome(pos).is(BiomeTags.HAS_VILLAGE_SNOWY);
            if (!isSnowy) {
                return;
            }

            // 6. 检查是否已经获得过该奖励（确保只触发一次）
            net.minecraft.nbt.CompoundTag persistentData = player.getPersistentData();
            net.minecraft.nbt.CompoundTag persistentTag;

            if (!persistentData.contains(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                persistentTag = new net.minecraft.nbt.CompoundTag();
                persistentData.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persistentTag);
            } else {
                persistentTag = persistentData.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
            }

            if (persistentTag.getBoolean("HighSnowyKillRewardClaimed")) {
                return;
            }

            // 7. 达成条件，标记并给予奖励
            persistentTag.putBoolean("HighSnowyKillRewardClaimed", true);

            net.minecraft.world.item.Item rewardItemObj = ModItems.AUVIOTRE_ITEM.get();
            if (rewardItemObj != null) {
                net.minecraft.world.item.ItemStack rewardItem = new net.minecraft.world.item.ItemStack(rewardItemObj, 1);
                if (!player.getInventory().add(rewardItem)) {
                    player.drop(rewardItem, false);
                }

                // 视听反馈
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);
            }
        }
    }

    // ---------------------------------------------------------
    // 效果：在末地击杀一只小僵尸，达成一次性挑战获得物品
    // ---------------------------------------------------------
    @SubscribeEvent
    public static void onEndBabyZombieDeath(net.minecraftforge.event.entity.living.LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }

        // 1. 检查死亡的实体是否为小僵尸 (Zombie 且 isBaby())
        if (event.getEntity() instanceof net.minecraft.world.entity.monster.Zombie zombie && zombie.isBaby()) {
            net.minecraft.world.level.Level level = event.getEntity().level();

            // 2. 检查当前维度是否为末地 (The End)
            if (level.dimension() != net.minecraft.world.level.Level.END) {
                return;
            }

            // 3. 检查伤害来源是否为玩家
            net.minecraft.world.damagesource.DamageSource source = event.getSource();
            if (source.getEntity() instanceof net.minecraft.world.entity.player.Player player) {

                // 4. 检查是否已经获得过该奖励（确保只触发一次）
                net.minecraft.nbt.CompoundTag persistentData = player.getPersistentData();
                net.minecraft.nbt.CompoundTag persistentTag;

                if (!persistentData.contains(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                    persistentTag = new net.minecraft.nbt.CompoundTag();
                    persistentData.put(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG, persistentTag);
                } else {
                    persistentTag = persistentData.getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG);
                }

                if (persistentTag.getBoolean("EndBabyZombieRewardClaimed")) {
                    return;
                }

                // 5. 达成条件，标记并给予奖励
                persistentTag.putBoolean("EndBabyZombieRewardClaimed", true);

                net.minecraft.world.item.Item rewardItemObj = ModItems.AUV_STAFF.get();
                if (rewardItemObj != null) {
                    net.minecraft.world.item.ItemStack rewardItem = new net.minecraft.world.item.ItemStack(rewardItemObj, 1);
                    if (!player.getInventory().add(rewardItem)) {
                        player.drop(rewardItem, false);
                    }

                    // 视听反馈
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            net.minecraft.sounds.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 0.8F);

                }
            }
        }
    }

    // NBT 键名常量
    private static final String FISH_COUNT_KEY   = "FishKillCount";
    private static final String CREEPER_COUNT_KEY = "CreeperKillCount";
    private static final String PHANTOM_COUNT_KEY = "PhantomKillCount";
    private static final String REWARD_CLAIMED_KEY = "FishCreeperPhantomRewardClaimed";

    // 达成条件
    private static final int FISH_REQUIRED    = 20;
    private static final int CREEPER_REQUIRED = 10;
    private static final int PHANTOM_REQUIRED = 5;

    @SubscribeEvent
    public static void givemikula(LivingDeathEvent event) {
        // 0. 只在服务端处理
        Level level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }

        // 1. 伤害来源必须是玩家
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        Entity deadEntity = event.getEntity();

        // 2. 获取持久化 NBT（跨死亡保留）
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistentTag;
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistentTag = new CompoundTag();
            persistentData.put(Player.PERSISTED_NBT_TAG, persistentTag);
        } else {
            persistentTag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        }

        // 3. 如果已经领取过奖励，不再累计
        if (persistentTag.getBoolean(REWARD_CLAIMED_KEY)) {
            return;
        }

        // 4. 根据击杀的实体类型累加计数
        boolean counted = false;
        if (deadEntity instanceof AbstractFish) {
            int count = persistentTag.getInt(FISH_COUNT_KEY) + 1;
            persistentTag.putInt(FISH_COUNT_KEY, count);
            counted = true;
        } else if (deadEntity instanceof Creeper) {
            int count = persistentTag.getInt(CREEPER_COUNT_KEY) + 1;
            persistentTag.putInt(CREEPER_COUNT_KEY, count);
            counted = true;
        } else if (deadEntity instanceof Phantom) {
            int count = persistentTag.getInt(PHANTOM_COUNT_KEY) + 1;
            persistentTag.putInt(PHANTOM_COUNT_KEY, count);
            counted = true;
        }

        if (!counted) {
            return;
        }

        // 5. 检查是否全部达成
        boolean fishDone    = persistentTag.getInt(FISH_COUNT_KEY)    >= FISH_REQUIRED;
        boolean creeperDone = persistentTag.getInt(CREEPER_COUNT_KEY) >= CREEPER_REQUIRED;
        boolean phantomDone = persistentTag.getInt(PHANTOM_COUNT_KEY) >= PHANTOM_REQUIRED;

        if (!(fishDone && creeperDone && phantomDone)) {
            return;
        }

        // 6. 标记已领取并发放奖励
        persistentTag.putBoolean(REWARD_CLAIMED_KEY, true);

        Item rewardItemObj = ModItems.MIKULA.get(); // 替换为你的奖励物品
        if (rewardItemObj != null) {
            ItemStack rewardItem = new ItemStack(rewardItemObj, 1);
            if (!player.getInventory().add(rewardItem)) {
                player.drop(rewardItem, false);
            }
        }
    }

    // NBT 键名常量
    private static final String CAT_COUNT_KEY       = "CatKillCount";
    private static final String REWARD_CLAIMED_KEY_BAIZELI = "CatKillRewardClaimed";

    // 达成条件
    private static final int CAT_REQUIRED = 20;

    @SubscribeEvent
    public static void givebaizeli(LivingDeathEvent event) {
        // 0. 只在服务端处理
        Level level = event.getEntity().level();
        if (level.isClientSide()) {
            return;
        }

        // 1. 伤害来源必须是玩家
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // 2. 判断死亡的实体是否为猫
        Entity deadEntity = event.getEntity();
        if (!(deadEntity instanceof Cat)) {
            return;
        }

        // 3. 获取持久化 NBT（跨死亡保留）
        CompoundTag persistentData = player.getPersistentData();
        CompoundTag persistentTag;
        if (!persistentData.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            persistentTag = new CompoundTag();
            persistentData.put(Player.PERSISTED_NBT_TAG, persistentTag);
        } else {
            persistentTag = persistentData.getCompound(Player.PERSISTED_NBT_TAG);
        }

        // 4. 如果已经领取过奖励，不再累计
        if (persistentTag.getBoolean(REWARD_CLAIMED_KEY_BAIZELI)) {
            return;
        }

        // 5. 累加击杀计数
        int catCount = persistentTag.getInt(CAT_COUNT_KEY) + 1;
        persistentTag.putInt(CAT_COUNT_KEY, catCount);

        // 6. 未达到目标，仅记录计数即可
        if (catCount < CAT_REQUIRED) {
            return;
        }

        // 7. 达成条件，标记并给予奖励
        persistentTag.putBoolean(REWARD_CLAIMED_KEY_BAIZELI, true);

        Item rewardItemObj = ModItems.BAI_ZE_LI.get(); // 替换为你的奖励物品
        if (rewardItemObj != null) {
            ItemStack rewardItem = new ItemStack(rewardItemObj, 1);
            if (!player.getInventory().add(rewardItem)) {
                player.drop(rewardItem, false);
            }

        }
    }

}