package com.first.themasses.Client.overlay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public class OverlayType {
    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final float width;
    private final float height;
    private final OverlayCondition condition;
    private final boolean billboard;
    private final OffsetFunc offsetY;
    private final ScaleFunc scale;
    private final int lifetime;
    private final boolean autoRemove;

    private OverlayType(Builder b) {
        this.id = b.id;
        this.texture = b.texture;
        this.width = b.width;
        this.height = b.height;
        this.condition = b.condition;
        this.billboard = b.billboard;
        this.offsetY = b.offsetY != null ? b.offsetY : (e, a, p) -> e.getBbHeight() + 0.5f;
        this.scale = b.scale != null ? b.scale : (e, a, p) -> 1.0f;
        this.lifetime = b.lifetime;
        this.autoRemove = b.autoRemove;
    }

    public static Builder builder(ResourceLocation id, ResourceLocation texture) {
        return new Builder(id, texture);
    }

    public ResourceLocation getId() {
        return id;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public OverlayCondition getCondition() {
        return condition;
    }

    public boolean isBillboard() {
        return billboard;
    }

    public float getOffsetY(LivingEntity e, int age, float pt) {
        return offsetY.getOffsetY(e, age, pt);
    }

    public float getScale(LivingEntity e, int age, float pt) {
        return scale.getScale(e, age, pt);
    }

    public int getLifetime() {
        return lifetime;
    }

    public boolean isAutoRemove() {
        return autoRemove;
    }

    @FunctionalInterface
    public interface ScaleFunc {
        float getScale(LivingEntity entity, int age, float partialTick);
    }

    @FunctionalInterface
    public interface OffsetFunc {
        float getOffsetY(LivingEntity entity, int age, float partialTick);
    }

    public static class Builder {
        private final ResourceLocation id;
        private final ResourceLocation texture;
        private float width = 0.5f;
        private float height = 0.5f;
        private OverlayCondition condition = OverlayCondition.always();
        private boolean billboard = true;
        private OffsetFunc offsetY;
        private ScaleFunc scale;
        private int lifetime = -1;
        private boolean autoRemove = true;

        Builder(ResourceLocation id, ResourceLocation texture) {
            this.id = id;
            this.texture = texture;
        }

        public Builder size(float w, float h) {
            this.width = w;
            this.height = h;
            return this;
        }

        public Builder condition(OverlayCondition c) {
            this.condition = c;
            return this;
        }

        public Builder billboard(boolean v) {
            this.billboard = v;
            return this;
        }

        public Builder offsetY(OffsetFunc fn) {
            this.offsetY = fn;
            return this;
        }

        public Builder scale(ScaleFunc fn) {
            this.scale = fn;
            return this;
        }

        public Builder lifetime(int ticks) {
            this.lifetime = ticks;
            return this;
        }

        public Builder autoRemove(boolean v) {
            this.autoRemove = v;
            return this;
        }

        public OverlayType build() {
            return new OverlayType(this);
        }
    }
}
