package com.first.themasses.Mixin;

import com.first.themasses.Config;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//boss标签
@Mixin(EntityType.class)
public class BossTagMixin {

    /**
     * 拦截 EntityType.is(TagKey) 方法
     * 检查目标标签是否为原版 Boss 标签，并对比配置文件中的生物名单
     */
    @Inject(method = "is(Lnet/minecraft/tags/TagKey;)Z", at = @At("HEAD"), cancellable = true)
    private void injectBossTag(TagKey<EntityType<?>> tag, CallbackInfoReturnable<Boolean> cir) {
        // 1. 确认检查的是否为 Forge/原版定义的 BOSSES 标签
        if (tag.equals(Tags.EntityTypes.BOSSES)) {
            EntityType<?> type = (EntityType<?>) (Object) this;

            // 2. 从 Config 类中获取动态加载的 Boss 集合
            if (Config.customBosses != null && Config.customBosses.contains(type)) {
                cir.setReturnValue(true);
            }
        }
    }
}