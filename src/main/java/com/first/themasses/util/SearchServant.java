package com.first.themasses.util;

import com.first.themasses.TheMasses;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SearchServant {
    private static final Map<UUID, ServantData> PLAYER_SERVANT_CACHE = new ConcurrentHashMap<>();
    private static final Map<UUID, EnhancedServantData> ENHANCED_PLAYER_CACHE = new ConcurrentHashMap<>();
    private static final int SCAN_INTERVAL_TICKS = 3600;
    private static int globalTickCounter = 0;

    // --- 反射缓存区 ---
    private static Class<?> IOwnedClass = null;
    private static Method getTrueOwnerMethod = null;

    static {
        try {
            // 动态加载类，如果不存在会抛出异常并被捕获
            IOwnedClass = Class.forName("com.Polarice3.Goety.api.entities.IOwned");
            getTrueOwnerMethod = IOwnedClass.getMethod("getTrueOwner");
        } catch (Exception ignored) {
            // 说明没有安装 Goety 模组，相关变量保持 null
        }
    }

    /**
     * 通用的所有者检测工具，支持原版 Ownable 和 Goety 反射
     */
    private static LivingEntity getEntityOwner(LivingEntity entity) {
        if (entity == null) return null;

        // 1. 尝试原版 OwnableEntity (使用 Entity 变量过渡，避开 IDE 冗余警告)
        if (entity instanceof OwnableEntity ownable) {
            Entity ownerEntity = ownable.getOwner();
            if (ownerEntity instanceof LivingEntity livingOwner) {
                return livingOwner;
            }
        }

        // 2. 尝试反射调用 Goety (保持不变)
        if (IOwnedClass != null && IOwnedClass.isInstance(entity)) {
            try {
                Object owner = getTrueOwnerMethod.invoke(entity);
                if (owner instanceof LivingEntity livingOwner) {
                    return livingOwner;
                }
            } catch (Exception e) {
                // 静默处理反射异常
            }
        }
        return null;
    }

    // --- 数据内部类 (保持原样) ---

    public static class ServantData {
        public final UUID playerUUID;
        public final Set<UUID> servantUUIDs;
        public int maxServants;
        public int lastScanTick;

        public ServantData(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.servantUUIDs = new HashSet<>();
            this.maxServants = 256;
            this.lastScanTick = 0;
        }

        public void addServant(UUID servantUUID) { servantUUIDs.add(servantUUID); }
        public void removeServant(UUID servantUUID) { servantUUIDs.remove(servantUUID); }
        public void clear() { servantUUIDs.clear(); }
        public int getServantCount() { return servantUUIDs.size(); }
    }

    public static class ServantDetailData {
        public final UUID servantUUID;
        public String entityType;
        public EquipmentData equipment;
        public AttributeData attributes;
        public long lastUpdateTime;

        public ServantDetailData(UUID servantUUID) {
            this.servantUUID = servantUUID;
            this.equipment = new EquipmentData();
            this.attributes = new AttributeData();
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public void updateFromEntity(LivingEntity entity) {
            this.entityType = entity.getType().getDescriptionId();
            this.equipment.updateFromEntity(entity);
            this.attributes.updateFromEntity(entity);
            this.lastUpdateTime = System.currentTimeMillis();
        }

        public static class EquipmentData {
            public ItemStack helmet = ItemStack.EMPTY;
            public ItemStack chestplate = ItemStack.EMPTY;
            public ItemStack leggings = ItemStack.EMPTY;
            public ItemStack boots = ItemStack.EMPTY;
            public ItemStack mainHand = ItemStack.EMPTY;
            public ItemStack offHand = ItemStack.EMPTY;

            public void updateFromEntity(LivingEntity entity) {
                this.helmet = entity.getItemBySlot(EquipmentSlot.HEAD).copy();
                this.chestplate = entity.getItemBySlot(EquipmentSlot.CHEST).copy();
                this.leggings = entity.getItemBySlot(EquipmentSlot.LEGS).copy();
                this.boots = entity.getItemBySlot(EquipmentSlot.FEET).copy();
                this.mainHand = entity.getMainHandItem().copy();
                this.offHand = entity.getOffhandItem().copy();
            }

            public Map<String, Object> toMap() {
                Map<String, Object> map = new HashMap<>();
                map.put("helmet", serializeItemStack(helmet));
                map.put("chestplate", serializeItemStack(chestplate));
                map.put("leggings", serializeItemStack(leggings));
                map.put("boots", serializeItemStack(boots));
                map.put("mainHand", serializeItemStack(mainHand));
                map.put("offHand", serializeItemStack(offHand));
                return map;
            }

            private Map<String, Object> serializeItemStack(ItemStack stack) {
                if (stack.isEmpty()) return null;
                Map<String, Object> itemData = new HashMap<>();
                itemData.put("item", stack.getItem().toString());
                itemData.put("count", stack.getCount());
                return itemData;
            }
        }

        public static class AttributeData {
            public double maxHealth;
            public double currentHealth;
            public double attackDamage;
            public void updateFromEntity(LivingEntity entity) {
                this.currentHealth = entity.getHealth();
                this.maxHealth = entity.getMaxHealth();
                AttributeInstance inst = entity.getAttribute(Attributes.ATTACK_DAMAGE);
                this.attackDamage = inst != null ? inst.getValue() : 0;
            }
            public Map<String, Double> toMap() {
                Map<String, Double> map = new HashMap<>();
                map.put("maxHealth", maxHealth);
                map.put("currentHealth", currentHealth);
                map.put("attackDamage", attackDamage);
                return map;
            }
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("servantUUID", servantUUID.toString());
            map.put("entityType", entityType);
            map.put("equipment", equipment.toMap());
            map.put("attributes", attributes.toMap());
            map.put("lastUpdateTime", lastUpdateTime);
            return map;
        }
    }

    public static class EnhancedServantData {
        public final UUID playerUUID;
        public final Map<UUID, ServantDetailData> servantDetails;

        public EnhancedServantData(UUID playerUUID) {
            this.playerUUID = playerUUID;
            this.servantDetails = new ConcurrentHashMap<>();
        }

        public void addOrUpdateServant(LivingEntity entity) {
            ServantDetailData detail = servantDetails.computeIfAbsent(entity.getUUID(), ServantDetailData::new);
            detail.updateFromEntity(entity);
        }

        public void removeServant(UUID servantUUID) { servantDetails.remove(servantUUID); }
        public void clear() { servantDetails.clear(); }
    }

    // --- 逻辑方法 ---

    public static Optional<EnhancedServantData> getEnhancedServantData(Player player) {
        return Optional.ofNullable(ENHANCED_PLAYER_CACHE.get(player.getUUID()));
    }

    public static Optional<ServantData> getServantData(Player player) {
        return Optional.ofNullable(PLAYER_SERVANT_CACHE.get(player.getUUID()));
    }

    public static void scanServantsForPlayer(ServerLevel level, ServerPlayer player) {
        ServantData data = PLAYER_SERVANT_CACHE.computeIfAbsent(player.getUUID(), ServantData::new);
        EnhancedServantData enhancedData = ENHANCED_PLAYER_CACHE.computeIfAbsent(player.getUUID(), EnhancedServantData::new);

        data.clear();
        enhancedData.clear();

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof LivingEntity livingEntity) {
                LivingEntity owner = getEntityOwner(livingEntity);
                if (owner != null && owner.getUUID().equals(player.getUUID())) {
                    data.addServant(livingEntity.getUUID());
                    enhancedData.addOrUpdateServant(livingEntity);
                }
            }
        }
        data.lastScanTick = globalTickCounter;
    }

    public static void scanAllPlayers(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            scanServantsForPlayer(level, player);
        }
    }

    public static void onServantJoin(Player player, LivingEntity servant) {
        PLAYER_SERVANT_CACHE.computeIfAbsent(player.getUUID(), ServantData::new).addServant(servant.getUUID());
        ENHANCED_PLAYER_CACHE.computeIfAbsent(player.getUUID(), EnhancedServantData::new).addOrUpdateServant(servant);
    }

    public static void onServantDeath(Player player, UUID servantUUID) {
        getServantData(player).ifPresent(data -> data.removeServant(servantUUID));
        getEnhancedServantData(player).ifPresent(data -> data.removeServant(servantUUID));
    }

    public static void updateServantData(LivingEntity servant) {
        LivingEntity owner = getEntityOwner(servant);
        if (owner instanceof Player ownerPlayer) {
            getEnhancedServantData(ownerPlayer).ifPresent(data -> data.addOrUpdateServant(servant));
        }
    }

    @Mod.EventBusSubscriber(modid = TheMasses.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class EventHandler {

        @SubscribeEvent
        public static void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.START) return;
            globalTickCounter++;
            if (globalTickCounter % SCAN_INTERVAL_TICKS == 0) {
                for (ServerLevel level : event.getServer().getAllLevels()) {
                    scanAllPlayers(level);
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
            if (!event.getEntity().level().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
                scanServantsForPlayer((ServerLevel) player.level(), player);
            }
        }

        @SubscribeEvent
        public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
            if (event.getLevel().isClientSide() || !(event.getEntity() instanceof LivingEntity living)) return;
            LivingEntity owner = getEntityOwner(living);
            if (owner instanceof Player p) onServantJoin(p, living);
        }

        @SubscribeEvent
        public static void onLivingDeath(LivingDeathEvent event) {
            if (event.getEntity().level().isClientSide()) return;
            LivingEntity owner = getEntityOwner(event.getEntity());
            if (owner instanceof Player p) onServantDeath(p, event.getEntity().getUUID());
        }

        @SubscribeEvent
        public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
            if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide()) return;
            if (event.player instanceof ServerPlayer player) {
                // 仅每秒更新一次，减轻全实体扫描压力
                if (player.level().getGameTime() % 20 == 0) {
                    ServerLevel level = (ServerLevel) player.level();
                    for (Entity entity : level.getAllEntities()) {
                        if (entity instanceof LivingEntity living) {
                            LivingEntity owner = getEntityOwner(living);
                            if (owner == player) {
                                updateServantData(living);
                            }
                        }
                    }
                }
            }
        }
    }
}