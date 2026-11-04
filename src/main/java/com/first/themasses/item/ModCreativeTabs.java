package com.first.themasses.item;

import com.first.themasses.TheMasses; // 替换为你的主类路径
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

public class ModCreativeTabs {
    // 1. 定义注册器
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "themasses");

    /** 不显示在创造模式物品栏的物品列表 */
    private static final Set<RegistryObject<? extends Item>> HIDDEN_ITEMS = Set.of(
            ModItems.SIM   // ★ 隐藏 SIM
    );

    // 2. 注册具体的物品栏
    public static final RegistryObject<CreativeModeTab> THE_MASSES_TAB = CREATIVE_MODE_TABS.register("the_masses_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.WU1WU2.get())) // 物品栏图标，用你的白泽饰品
                    .title(Component.translatable("creativetab.the_masses_tab")) // 语言文件中的标题
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().forEach(entry -> {
                            if (entry == ModItems.SIM) {
                                return;
                            }
                            output.accept(entry.get());
                        });
                    })
                    .build());

    // 4. 被主类调用
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}