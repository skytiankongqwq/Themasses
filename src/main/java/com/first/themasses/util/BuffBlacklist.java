    package com.first.themasses.util;

    import net.minecraft.resources.ResourceLocation;
    import net.minecraft.world.effect.MobEffect;
    import net.minecraft.world.effect.MobEffects;
    import net.minecraftforge.registries.ForgeRegistries;
    import java.util.HashSet;
    import java.util.Set;

    public class BuffBlacklist {
        private static final Set<MobEffect> BLACKLIST = new HashSet<>();

        static {
            BLACKLIST.add(MobEffects.DAMAGE_RESISTANCE);
            // 软兼容注入示例：如果存在对应 Mod，则把特定 buff 加入黑名单
            safeAdd("some_op_mod", "instant_death");
            safeAdd("another_mod", "infinite_loop_effect");
            safeAdd("jerotesvillage", "fairness");
        }

        private static void safeAdd(String modid, String name) {
            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(modid, name));
            if (effect != null) {
                BLACKLIST.add(effect);
            }
        }

        public static boolean isBlacklisted(MobEffect effect) {
            return BLACKLIST.contains(effect);
        }
    }