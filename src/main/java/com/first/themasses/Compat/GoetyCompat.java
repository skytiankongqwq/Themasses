package com.first.themasses.Compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import java.lang.reflect.Method;
import java.util.UUID;

public class GoetyCompat {

    /**
     * 只有当 Goety 加载时，才会通过反射尝试获取所有者
     */
    public static UUID getGoetyOwnerUUID(LivingEntity entity) {
        // 第一道防线：没装 Mod 直接返回 null
        if (!ModList.get().isLoaded("goety")) return null;

        try {
            // 使用字符串加载类，绕过编译器检查
            Class<?> iOwnedClass = Class.forName("com.Polarice3.Goety.api.entities.IOwned");

            // 判断实体是否实现了这个接口
            if (iOwnedClass.isInstance(entity)) {
                // 反射获取 getTrueOwner 方法
                Method getTrueOwner = iOwnedClass.getMethod("getTrueOwner");
                Object owner = getTrueOwner.invoke(entity);

                if (owner instanceof LivingEntity livingOwner) {
                    return livingOwner.getUUID();
                }
            }
        } catch (Exception e) {
            // 如果 Goety 版本变动导致方法名不对，这里会捕获异常并静默失败
        }
        return null;
    }

    public static Player getGoetyOwnerPlayer(LivingEntity entity) {
        if (!ModList.get().isLoaded("goety")) return null;

        try {
            Class<?> iOwnedClass = Class.forName("com.Polarice3.Goety.api.entities.IOwned");
            if (iOwnedClass.isInstance(entity)) {
                Method getTrueOwner = iOwnedClass.getMethod("getTrueOwner");
                Object owner = getTrueOwner.invoke(entity);

                if (owner instanceof Player player) {
                    return player;
                }
            }
        } catch (Exception e) {
            // 静默失败
        }
        return null;
    }
}