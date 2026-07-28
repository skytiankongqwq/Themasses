package com.first.themasses.events.mysterious;

import com.first.themasses.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Comparator;
import java.util.UUID;
import java.util.List;

@SuppressWarnings("removal")
@Mod.EventBusSubscriber(modid = "themasses")
public class SimEvents {

    private static final UUID SIM_STATS_UUID = UUID.fromString("11223344-5566-7788-9900-aabbccddeeff");

    private static Item getSimItem() {
        return ModItems.SIM.get();
    }

    private static boolean hasSim(Player player) {
        if (player == null) return false;
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.SIM.get()).isPresent();
    }

    // 在你的 SimEvents 类中添加（确保这部分只在客户端加载，或者在方法内判断 side）
    @SubscribeEvent
    public static void onComputeCameraAngles(net.minecraftforge.client.event.ViewportEvent.ComputeCameraAngles event) {
        @SuppressWarnings("resource")
        Player player = net.minecraft.client.Minecraft.getInstance().player;

        // 只要带着饰品，就强行锁定视角矩阵
        if (player != null && hasSim(player)) {
            // 关键点：将 player 实例的受击时间强制重置为 0
            // ViewportEvent 触发时，渲染器正准备计算视角倾斜
            // 此时设为 0，渲染器会认为玩家没有受伤，从而跳过所有 Camera Tilt 逻辑
            player.hurtTime = 0;
            player.hurtDuration = 0;
        }
    }
    // ================== 新主动技能逻辑：三种情况 (已改为持续开关) ==================
    //击杀生物
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDrops(LivingDropsEvent event) {
        if (event.getSource().getEntity() instanceof Player player && isLucky(player)) {
            LivingEntity victim = event.getEntity();
            if (!(victim.level() instanceof ServerLevel serverLevel)) return;

            // 1. 清空原生随机，我们要手动发货
            event.getDrops().clear();

            // 2. 强制掉落原本穿戴的装备
            victim.getArmorSlots().forEach(s -> addRawStack(event, victim, s));
            victim.getHandSlots().forEach(s -> addRawStack(event, victim, s));

            try {
                ResourceLocation loc = victim.getLootTable();
                // 直接获取 LootTable 对象
                LootTable table = serverLevel.getServer().getLootData().getLootTable(loc);

                // 【关键修改】绕过 Registries，直接使用 LootDataType 序列化 table 对象
                // 这样 100% 不会报 LOOT_TABLE 的错
                com.google.gson.JsonElement jsonElement = net.minecraft.world.level.storage.loot.LootDataType.TABLE.parser().toJsonTree(table);

                if (jsonElement != null && jsonElement.isJsonObject()) {
                    String jsonStr = jsonElement.toString();
                    // 用正则表达式暴力抓取 JSON 里所有的物品 ID
                    // 这种匹配方式能精准锁定所有 "name": "..." 里的物品
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                    java.util.regex.Matcher matcher = pattern.matcher(jsonStr);

                    java.util.Set<String> foundIds = new java.util.HashSet<>();
                    while (matcher.find()) {
                        String id = matcher.group(1);

                        // 过滤掉非物品项（比如嵌套表或者某些特殊函数名）
                        if (!id.contains("loot_tables") && !id.contains("functions") && foundIds.add(id)) {
                            // 利用 Forge 注册表直接把字符串 ID 变成 Item 对象
                            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                addRawStack(event, victim, new ItemStack(item));
                            }
                        }
                    }
                }

                player.displayClientMessage(Component.literal("§b[仿真] §f因果坍缩：名册强制补完。"), true);
                // 已移除 consume(player)，开关开启期间持续生效
            } catch (Exception e) {
                // 反正报错了我们就打印，不耽误程序运行
                e.printStackTrace();
            }
        }
    }

    private static void addRawStack(LivingDropsEvent event, LivingEntity victim, ItemStack stack) {
        if (!stack.isEmpty()) {
            event.getDrops().add(new ItemEntity(victim.level(), victim.getX(), victim.getY(), victim.getZ(), stack.copy()));
        }
    }

    private static boolean isLucky(Player p) {
        return p.getPersistentData().getBoolean("SimLuckyActive") &&
                CuriosApi.getCuriosHelper().findFirstCurio(p, ModItems.SIM.get()).isPresent();
    }

    // 情况 2：附魔时获取所有可能的最高级附魔
    @SubscribeEvent
    public static void onPlayerTickEnchant(TickEvent.PlayerTickEvent event) {
        Player player = event.player;
        if (player.level().isClientSide || !hasSim(player)) return;

        // 如果玩家开启了附魔台界面，并且处于技能激活状态
        if (player.containerMenu instanceof EnchantmentMenu menu && player.getPersistentData().getBoolean("SimLuckyActive")) {
            ItemStack itemToEnchant = menu.getSlot(0).getItem();

            // 只要玩家点了附魔（通过检测附魔台消耗判定，或者检测槽位变化）
            // 这里我们采用最暴力的：一旦检测到技能开启且附魔台有东西，直接“强行灌注”
            if (!itemToEnchant.isEmpty() && itemToEnchant.isEnchantable() && !itemToEnchant.isEnchanted()) {

                // 获取 30 级下所有可能的附魔
                List<EnchantmentInstance> list = EnchantmentHelper.getAvailableEnchantmentResults(30, itemToEnchant, true);

                for (EnchantmentInstance inst : list) {
                    // 核心要求：只要能放进去，哪怕冲突也附魔上
                    // 我们直接使用 ItemStack 的底层方法增加附魔
                    if (inst.enchantment.canEnchant(itemToEnchant)) {
                        itemToEnchant.enchant(inst.enchantment, inst.level);
                    }
                }

                player.displayClientMessage(Component.literal("§b[仿真] §f因果坍缩：已强行注入全部可能性。"), true);
                // 已移除 consumeSimAbility(player)，开关开启期间持续生效
            }
        }
    }

    // 针对箱子：拦截玩家点击方块的行为，在箱子抽奖前强行注入
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockInteract(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        // 只有服务端处理，且必须有技能标记
        if (!player.level().isClientSide && isLucky(player)) {
            BlockPos pos = event.getPos();
            // 检查是否是容器 (如箱子、木桶等)
            if (player.level().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity container) {

                // 反射获取战利品表路径 (1.20.1 SRG: f_59605_)
                ResourceLocation lootTableLoc = ObfuscationReflectionHelper.getPrivateValue(
                        net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity.class, container, "f_59605_");

                if (lootTableLoc != null) {
                    ServerLevel serverLevel = (ServerLevel) player.level();
                    // 拿到战利品表对象
                    LootTable table = serverLevel.getServer().getLootData().getLootTable(lootTableLoc);

                    // 1. 彻底清除容器原有的抽奖逻辑和内容
                    container.setLootTable(null, 0);
                    container.clearContent();

                    // 2. 暴力 JSON 解析提取名册
                    try {
                        com.google.gson.JsonElement jsonElement = net.minecraft.world.level.storage.loot.LootDataType.TABLE.parser().toJsonTree(table);
                        if (jsonElement != null && jsonElement.isJsonObject()) {
                            String jsonStr = jsonElement.toString();
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                            java.util.regex.Matcher matcher = pattern.matcher(jsonStr);

                            java.util.Set<String> allFoundIds = new java.util.LinkedHashSet<>(); // 保持发现顺序
                            while (matcher.find()) {
                                String id = matcher.group(1);
                                if (!id.contains("loot_tables") && !id.contains("functions")) {
                                    allFoundIds.add(id);
                                }
                            }

                            // --- 核心改动：优先级排序 ---
                            java.util.List<String> sortedIds = new java.util.ArrayList<>();
                            java.util.List<String> minecraftIds = new java.util.ArrayList<>();

                            for (String id : allFoundIds) {
                                if (id.startsWith("minecraft:")) {
                                    minecraftIds.add(id);
                                } else {
                                    sortedIds.add(id); // 模组物品排在前面
                                }
                            }
                            // 将原版物品追加到模组物品后面
                            sortedIds.addAll(minecraftIds);

                            // --- 执行填装 ---
                            int slot = 0;
                            int maxSlots = container.getContainerSize(); // 或者是 containerEntity.getContainerSize()
                            for (String id : sortedIds) {
                                if (slot >= maxSlots) break; // 格子满了就停止

                                Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                    container.setItem(slot++, new net.minecraft.world.item.ItemStack(item));
                                }
                            }

                            // 发送提示
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§f因果坍缩：名册已按优先级重构。"), true);
                            // 已移除 consume(player)，开关开启期间持续生效
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // 针对实体容器：如箱子矿车、箱子船、甚至驮着箱子的驴
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && player instanceof ServerPlayer serverPlayer && isLucky(player)) {
            Entity target = event.getTarget();

            if (target instanceof ContainerEntity targetContainer) {
                ResourceLocation lootTableLoc = targetContainer.getLootTable();
                if (lootTableLoc == null) return;

                try {
                    ServerLevel serverLevel = (ServerLevel) player.level();
                    LootTable table = serverLevel.getServer().getLootData().getLootTable(lootTableLoc);
                    com.google.gson.JsonElement jsonElement = net.minecraft.world.level.storage.loot.LootDataType.TABLE.parser().toJsonTree(table);

                    if (jsonElement != null && jsonElement.isJsonObject()) {
                        // --- 1. 预解析物品列表 ---
                        java.util.List<ItemStack> finalItems = new java.util.ArrayList<>();
                        String jsonStr = jsonElement.toString();
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"name\"\\s*:\\s*\"([^\"]+)\"");
                        java.util.regex.Matcher matcher = pattern.matcher(jsonStr);
                        java.util.Set<String> allFoundIds = new java.util.LinkedHashSet<>();
                        while (matcher.find()) {
                            String id = matcher.group(1);
                            if (!id.contains("loot_tables") && !id.contains("functions")) allFoundIds.add(id);
                        }
                        java.util.List<String> sortedIds = new java.util.ArrayList<>();
                        java.util.List<String> mIds = new java.util.ArrayList<>();
                        for (String id : allFoundIds) {
                            if (id.startsWith("minecraft:")) mIds.add(id); else sortedIds.add(id);
                        }
                        sortedIds.addAll(mIds);
                        for (String id : sortedIds) {
                            Item item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                finalItems.add(new ItemStack(item));
                            }
                        }

                        // --- 2. 尝试 Lootr 分流 ---
                        boolean handledByLootr = false;
                        if (net.minecraftforge.fml.ModList.get().isLoaded("lootr")) {
                            handledByLootr = new com.first.themasses.Compat.LootrCompatImpl()
                                    .doLootrRefactor(serverLevel, target, targetContainer, serverPlayer, finalItems, lootTableLoc);
                        }

                        // --- 3. 物理填装 (仅在 Lootr 未处理时执行) ---
                        if (!handledByLootr) {
                            targetContainer.setLootTable(null);
                            targetContainer.setLootTableSeed(0);
                            targetContainer.clearContent();
                            int slot = 0;
                            for (ItemStack stack : finalItems) {
                                if (slot >= targetContainer.getContainerSize()) break;
                                targetContainer.setItem(slot++, stack);
                            }
                        } else {
                            // Lootr 成功处理：取消原版事件，防止打开物理实体
                            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                            event.setCanceled(true);
                        }

                        player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b[仿真] §f因果坍缩：载具名册已重构。"), true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // ================== 核心算法：寻找距离指针(准星)最近的生物 ==================
    private static LivingEntity findBestTarget(Player player, double maxRange) {
        Vec3 lookVec = player.getLookAngle();
        Vec3 startPos = player.getEyePosition();
        AABB searchBox = player.getBoundingBox().inflate(maxRange);

        LivingEntity bestTarget = null;
        double closestDistToRay = Double.MAX_VALUE;

        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, searchBox)) {
            if (target == player || !target.isAlive() || target.isAlliedTo(player)) continue;

            Vec3 toTarget = target.position().subtract(startPos);
            // 确保怪物在玩家视角的前半球（防背后锁定）
            if (toTarget.dot(lookVec) < 0) continue;

            // 叉乘计算距离准星射线的垂直距离（实现“距离指针最近”）
            double distToRay = toTarget.cross(lookVec).length();
            if (distToRay < closestDistToRay && toTarget.length() <= maxRange) {
                closestDistToRay = distToRay;
                bestTarget = target;
            }
        }
        return bestTarget;
    }


    // ================== 被动：怠战 (周围怪物属性变化) ==================
    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) return;

        // 如果该实体是敌对怪物
        if (entity instanceof Enemy) {
            // 搜索 32 格内是否有带 Sim 饰品的玩家
            Player nearbySimPlayer = entity.level().getNearestPlayer(entity, 32.0D);
            boolean hasAura = nearbySimPlayer != null && hasSim(nearbySimPlayer);

            // 将属性变化应用给怪物（提升生存，降低攻击）
            manageAttribute(entity, Attributes.MAX_HEALTH, 0.1, hasAura);
            manageAttribute(entity, Attributes.ARMOR, 0.1, hasAura);
            manageAttribute(entity, Attributes.ARMOR_TOUGHNESS, 0.1, hasAura);
            manageAttribute(entity, Attributes.MOVEMENT_SPEED, 0.1, hasAura);
            manageAttribute(entity, Attributes.ATTACK_DAMAGE, -0.15, hasAura); // 降低 15% 攻击力

            // 怪物生命回复：如果没有到达上限，每 2 秒回复 1 点血量
            if (hasAura && entity.tickCount % 40 == 0 && entity.getHealth() < entity.getMaxHealth()) {
                entity.heal(1.0F);
            }
        }
    }

    private static void manageAttribute(LivingEntity entity, Attribute attribute, double value, boolean enable) {
        var inst = entity.getAttribute(attribute);
        if (inst != null) {
            boolean hasModifier = inst.hasModifier(new AttributeModifier(SIM_STATS_UUID, "SimStat", value, AttributeModifier.Operation.MULTIPLY_TOTAL));
            if (enable && !hasModifier) {
                inst.addTransientModifier(new AttributeModifier(SIM_STATS_UUID, "SimStat", value, AttributeModifier.Operation.MULTIPLY_TOTAL));
            } else if (!enable && hasModifier) {
                inst.removeModifier(SIM_STATS_UUID);
            }
        }
    }

    // ================== 被动：怠战 (降低仇恨 & 受击弹飞) ==================
    @SubscribeEvent
    public static void onLivingVisibility(LivingEvent.LivingVisibilityEvent event) {
        if (event.getEntity() instanceof Player player && hasSim(player)) {
            if (event.getLookingEntity() instanceof Enemy && !event.getLookingEntity().getType().is(Tags.EntityTypes.BOSSES)) {
                event.modifyVisibility(0.1);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof Player player && hasSim(player)) {
            if (event.getSource().getEntity() instanceof LivingEntity attacker) {
                // 修复弹飞方向：用 玩家坐标 减去 攻击者坐标，底层 knockback 取反后即为向外推
                double d0 = player.getX() - attacker.getX();
                double d1 = player.getZ() - attacker.getZ();
                attacker.knockback(1.5F, d0, d1);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide || !hasSim(player)) return;

        if (player.swingTime == 1) {
            // 1. 扫描 8 格最近目标
            List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getBoundingBox().inflate(8.0),
                    entity -> entity != player && entity.isAlive());

            LivingEntity target = targets.stream()
                    .min(Comparator.comparingDouble(player::distanceToSqr))
                    .orElse(null);

            if (target != null) {
                // 2. 记录当前的疾跑状态
                boolean wasSprinting = player.isSprinting();

                // 3. 强制满蓄力 (使用反射确保 100% 伤害)
                try {
                    java.lang.reflect.Field ticker = LivingEntity.class.getDeclaredField("f_20911_");
                    ticker.setAccessible(true);
                    ticker.setInt(player, 100);
                } catch (Exception e) {
                    try {
                        java.lang.reflect.Field ticker = LivingEntity.class.getDeclaredField("attackStrengthTicker");
                        ticker.setAccessible(true);
                        ticker.setInt(player, 100);
                    } catch (Exception ignored) {}
                }

                // 4. 执行原版攻击逻辑 (这里会导致疾跑消失)
                player.attack(target);

                // 5. 【修复核心】如果攻击前在疾跑，现在强行恢复它
                if (wasSprinting) {
                    player.setSprinting(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onProjectileJoin(EntityJoinLevelEvent event) {
        // 当箭矢、雪球、火球等弹射物生成的瞬间
        if (event.getEntity() instanceof Projectile projectile && !projectile.level().isClientSide) {
            if (projectile.getOwner() instanceof Player player && hasSim(player)) {
                // 搜索 64 格内的最佳目标
                LivingEntity target = findBestTarget(player, 64.0);

                if (target != null) {
                    // 获取弹射物原本的飞行速度
                    double speed = projectile.getDeltaMovement().length();
                    if (speed < 0.1) speed = 1.5; // 给个保底速度

                    // 计算瞄准目标胸口的方向
                    Vec3 targetPos = target.getEyePosition().subtract(0, target.getBbHeight() * 0.5, 0);
                    Vec3 shootDir = targetPos.subtract(projectile.position()).normalize();

                    // 调用底层 shoot 方法，瞬间重写弹射物的轨道和朝向
                    projectile.shoot(shootDir.x, shootDir.y, shootDir.z, (float)speed, 0.0F);
                }
            }
        }
    }

    // ================== 被动：跑路啦兄弟跑路啦 ==================
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onFatalDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && hasSim(player)) {
            Item simItem = getSimItem();
            if (simItem != null && !player.getCooldowns().isOnCooldown(simItem)) {
                event.setCanceled(true);
                player.setHealth(player.getMaxHealth() * 0.2F);

                for (int i = 0; i < 16; i++) {
                    double x = player.getX() + (player.getRandom().nextDouble() - 0.5D) * 32.0D;
                    double y = net.minecraft.util.Mth.clamp(
                            player.getY() + (double)(player.getRandom().nextInt(16) - 8),
                            (double)player.level().getMinBuildHeight(),
                            (double)(player.level().getMinBuildHeight() + ((net.minecraft.server.level.ServerLevel)player.level()).getLogicalHeight() - 1)
                    );
                    double z = player.getZ() + (player.getRandom().nextDouble() - 0.5D) * 32.0D;

                    if (player.randomTeleport(x, y, z, true)) {
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                        break;
                    }
                }

                player.getCooldowns().addCooldown(simItem, 2000);
                player.displayClientMessage(Component.literal("§d跑路啦兄弟跑路啦！"), true);
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 60, 4));
            }
        }
    }
}