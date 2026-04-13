package com.first.themasses.effect;

import com.first.themasses.TheMasses;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//buff注册
public class ModEffects {
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, TheMasses.MOD_ID);

    public static final RegistryObject<MobEffect> BAMBOOZLED = MOB_EFFECTS.register("bamboozled",
            () -> new BamboozledDebuffEffect(MobEffectCategory.HARMFUL, 0x47C49F));

    public static final RegistryObject<MobEffect> THINKING_QUIETLY = MOB_EFFECTS.register("thinking_quietly",
            () -> new ThinkingQuietlyEffect(MobEffectCategory.HARMFUL));

    public static final RegistryObject<MobEffect> ONE_IDEA = MOB_EFFECTS.register("one_idea",
             OneIdeaEffect::new);

    public static final RegistryObject<MobEffect> PARADOX_VIRUS = MOB_EFFECTS.register("paradox_virus",
            ParadoxVirusEffect::new);
    public static final RegistryObject<MobEffect> ZITANHUA = MOB_EFFECTS.register("zitanhua",
            ZitanhuaEffect::new);
}