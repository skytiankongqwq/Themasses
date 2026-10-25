package com.first.themasses;

import com.first.themasses.block.ModBlock;
import com.first.themasses.attributes.RegistryAttribute;
import com.first.themasses.command.MirrorTestCommand;
import com.first.themasses.command.title.ZhilingCommand;
import com.first.themasses.effect.ModEffects;
import com.first.themasses.effect.ModPotions;
import com.first.themasses.entity.ModEntities;
import com.first.themasses.events.buff.PlayerAntiGravityEvent;
import com.first.themasses.item.ModCreativeTabs;
import com.first.themasses.item.ModItems;
import com.first.themasses.loot.ModLootModifiers;
import com.first.themasses.networking.ModMessages;
import com.first.themasses.worldgen.structure.ModStructures;
import com.mojang.logging.LogUtils;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent; // 【新增导入】
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import com.first.themasses.item.food.goetyitem;

import static com.first.themasses.util.SoundRegistry.SOUNDS;
@SuppressWarnings("removal")
//主类
// The value here should match an entry in the META-INF/mods.toml file
@Mod(TheMasses.MOD_ID)
public class TheMasses {

    public static final String MOD_ID = "themasses";
    public static final Logger LOGGER = LogUtils.getLogger();

    // 创建 Deferred Register
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);

    public TheMasses(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        ModBlock.BLOCKS.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModEffects.MOB_EFFECTS.register(modEventBus);
        ModPotions.register(modEventBus);
        SOUNDS.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModStructures.register(modEventBus);
        ModLootModifiers.register(modEventBus);
        RegistryAttribute.ATTRIBUTES.register(modEventBus);


        ModMessages.register();

        MinecraftForge.EVENT_BUS.register(PlayerAntiGravityEvent.class);
        // 主类本身也监听事件，需要注册实例
        MinecraftForge.EVENT_BUS.register(this);

        // 注册 Creative Mode Tab 内容事件
        modEventBus.addListener(this::addCreative);

        // 注册 Mod 配置
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    /**
     * FMLCommonSetupEvent: Mod 通用设置 (Common Setup) 阶段。
     * 适合进行网络注册、Capabilities 注册、Food 属性修改等。
     */
    private void commonSetup(final FMLCommonSetupEvent event) {

        // 2. 线程安全地执行属性修改逻辑
        event.enqueueWork(() -> {
            // 调用你之前的物品属性修改方法（确保这个方法名没改错）
            goetyitem.registerGoetyFoodProperties();
        });

        // 3. (可选) 如果你想确认配置里的物品是否生效，可以保留这一行逻辑
        if (Config.items != null && !Config.items.isEmpty()) {
            Config.items.forEach((item) -> LOGGER.info("配置文件加载物品: {}", item.toString()));
        }
    }

    /**
     * FMLClientSetupEvent: Mod 客户端设置阶段。
     * 适合进行客户端事件注册、屏幕注册、渲染注册等。
     */
    private void clientSetup(final FMLClientSetupEvent event) {
        // EnqueueWork 确保它在客户端线程上执行
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(ModBlock.AUVIOTRE_BLOCK.get(), RenderType.cutout());
            // 【关键修正】在客户端设置阶段注册客户端事件，确保 ReverseControlsPacket::onClientInput 在正确环境被调用
            LOGGER.info("ReverseControlsPacket::onClientInput registered successfully on client.");
        });
    }


    // Add the example block items to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.OP_BLOCKS) {
            // 这里可以添加你的物品到创意标签
        }
    }

    @SubscribeEvent
    public void onCommandsRegister(RegisterCommandsEvent event) {
        ZhilingCommand.register(event.getDispatcher());
        MirrorTestCommand.register(event.getDispatcher());
    }//头衔类注册

    // You can use SubscribeEvent and let the Event Bus discover methods to call
}