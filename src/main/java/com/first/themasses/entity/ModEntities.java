package com.first.themasses.entity;

import com.first.themasses.entity.VoidPiercerProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.first.themasses.TheMasses.MOD_ID;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, MOD_ID);

    // 烟花苦力怕
    public static final RegistryObject<EntityType<FlashCreeper>> FLASH_CREEPER =
            ENTITY_TYPES.register("flash_creeper",
                    () -> EntityType.Builder.of(FlashCreeper::new, MobCategory.MONSTER) // 改为 MONSTER
                            .sized(0.6f, 1.7f)
                            .clientTrackingRange(8)
                            .build("flash_creeper"));

    // 小行星 (火球)
    public static final RegistryObject<EntityType<SmallFireBall>> SMALLFIRE_BALL =
            ENTITY_TYPES.register("smallfire_ball",
                    () -> EntityType.Builder.<SmallFireBall>of(SmallFireBall::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(4)
                            .updateInterval(10)
                            .build("smallfire_ball"));
    // --- 新增：虚空刺杀者投影 ---
    public static final RegistryObject<EntityType<VoidPiercerProjectileEntity>> VOID_PIERCER_PROJECTILE =
            ENTITY_TYPES.register("void_piercer_projectile",
                    () -> EntityType.Builder.<VoidPiercerProjectileEntity>of(VoidPiercerProjectileEntity::new, MobCategory.MISC)
                            .sized(0.5F, 0.5F) // 设置碰撞箱大小
                            .clientTrackingRange(4)
                            .updateInterval(20)
                            .build("void_piercer_projectile"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}