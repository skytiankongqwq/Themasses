package com.first.themasses.attributes;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

/**
 * 体型属性读取工具。从 Dream Sakura 的 attributes 包移植。
 */
public final class ScaleAttributeHelper {

    public static final double DEFAULT_SCALE = 1.0D;
    public static final double EPSILON = 1.0E-3D;
    public static final float PLAYER_STANDING_EYE_HEIGHT = 1.62F;

    private ScaleAttributeHelper() {
    }

    public static double getScale(LivingEntity entity) {
        if (entity == null || entity.getAttributes() == null) {
            return DEFAULT_SCALE;
        }

        AttributeInstance scaleAttr = entity.getAttribute(RegistryAttribute.SCALE.get());
        if (scaleAttr == null) {
            return DEFAULT_SCALE;
        }

        double scale = scaleAttr.getValue();
        return Double.isFinite(scale) ? scale : DEFAULT_SCALE;
    }

    public static boolean isDefault(double scale) {
        return Math.abs(scale - DEFAULT_SCALE) <= EPSILON;
    }

    public static float getScaledPlayerStandingEyeHeight(LivingEntity entity) {
        return PLAYER_STANDING_EYE_HEIGHT * (float) getScale(entity);
    }
}
