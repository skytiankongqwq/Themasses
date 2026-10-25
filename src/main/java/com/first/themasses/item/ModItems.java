package com.first.themasses.item;

import com.first.themasses.TheMasses;
import com.first.themasses.block.ModBlock;
import com.first.themasses.curios.all.MarbleShardItem;
import com.first.themasses.curios.mysterious.*;
import com.first.themasses.curios.ring.heishatangring;
import com.first.themasses.item.food.DoofGnawoalItem;
import com.first.themasses.item.food.ForbiddenFruitItem;
import com.first.themasses.item.food.LaowangFoodLight;
import com.first.themasses.item.food.SpiritLiquidVialItem;
import com.first.themasses.item.prop.*;
import com.first.themasses.item.sword.SoulBannerItem;
import com.first.themasses.item.sword.VoidPiercer;
import com.first.themasses.item.sword.Wu1wu2Sword;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

// 注册物品
public class ModItems {

    // 创建一个延迟注册器，用于注册物品
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TheMasses.MOD_ID);
    //灵魂木
    public static final RegistryObject<Item> SOUL_BLOCK_ITEM = ITEMS.register("soul_block",
            () -> new BlockItem(ModBlock.SOUL_BLOCK.get(), new Item.Properties()) {
                @Override
                public @NotNull Component getName(@NotNull ItemStack stack) {
                    return super.getName(stack).copy().withStyle(ChatFormatting.DARK_PURPLE);
                }
            });
    //老王食物
    public static final RegistryObject<Item> LAOWANG237_FOOD = ITEMS.register("laowang237_food",
            () -> new LaowangFoodLight(
                    new Item.Properties().food(ModFoods.LAOWANG_FOOD_PROPERTIES)
            ) {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //老王饰品
    public static final RegistryObject<Item> LAOWANG237 = ITEMS.register(
            "laowang237",
            () -> new laowang237(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //大智饰品
    public static final RegistryObject<Item> DAZHI = ITEMS.register(
            "dazhi",
            () -> new DaZhi(new Item.Properties().stacksTo(1))
    );
    //灵液瓶
    public static final RegistryObject<Item> SPIRIT_LIQUID_VIAL = ITEMS.register(
            "spirit_liquid_vial",
            () -> new SpiritLiquidVialItem(new Item.Properties()
                    .stacksTo(64)
                    .food(ModFoods.SPIRIT_LIQUID_VIAL)
            ) {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF00))); // 淡蓝色 (Aqua)
                }
            }
    );
    //抗火药水饰品
    public static final RegistryObject<Item> GREEN_HAND_POTF = ITEMS.register(
            "green_hand_potf",
            () -> new Kanghuoyaoshui(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
   //月灼饰品
    public static final RegistryObject<Item> LYUEZHUO = ITEMS.register(
            "lyuezhuo",
            () -> new Lyuezhuo(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //伊甸禁果
    public static final RegistryObject<Item> FORBIDDEN_FRUIT_OF_EDEN = ITEMS.register("forbidden_fruit_of_eden",
            () -> new ForbiddenFruitItem(
                    new Item.Properties().food(ModFoods.FORBIDDEN_FRUIT_OF_EDEN)
            ) {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //Auv饰品
    public static final RegistryObject<Item> AUVIOTRE_ITEM = ITEMS.register("laxauviotre",
            () -> new Auviotre(ModBlock.AUVIOTRE_BLOCK.get(),
                    new Item.Properties().stacksTo(1)){
                @Override
                public @NotNull Component getName(@NotNull ItemStack stack) {
                    return super.getName(stack).copy().withStyle(ChatFormatting.DARK_PURPLE);
                }
            }
    );
    //聚形之杖
    public static final RegistryObject<Item> AUV_STAFF = ITEMS.register("auv_staff",
            () -> new AuvStaff(new Item.Properties()
                    .stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //万魂幡
    public static final RegistryObject<Item> SOUL_BANNER = ITEMS.register("soul_banner",
            () -> new SoulBannerItem(new Item.Properties().fireResistant()) {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(ChatFormatting.DARK_RED);
                }
            }
    );
    //黑砂糖
    public static final RegistryObject<Item> HEISHATANG = ITEMS.register(
            "heishatang",
            () -> new heishatang(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //妙用之戒
    public static final RegistryObject<Item> HEISHATANGRING = ITEMS.register(
            "heishatangring",
            () -> new heishatangring(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //p7饰品
    public static final RegistryObject<Item> PLAYER__7 = ITEMS.register(
            "player__7",
            () -> new Playerseven(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //终之祈Final Prayer
    public static final RegistryObject<Item> FINAL_PRAYER = ITEMS.register("final_prayer",
            () -> new FinalPrayerItem(new Item.Properties()
                    .stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //昆仲饰品
    public static final RegistryObject<Item> KUN_ZHONG = ITEMS.register(
            "kun_zhong",
            () -> new KunZhong(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //腹切丸Harakiri Blade
    public static final RegistryObject<Item> HARAKIRI_BLADE = ITEMS.register(
            "harakiri_blade",
            () -> new HarikiriMaru(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(ChatFormatting.DARK_RED);
                }
            }
    );
    //白仄黎饰品
    public static final RegistryObject<Item> BAI_ZE_LI = ITEMS.register(
            "bai_ze_li",
            () -> new Baizeli(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //瓦克因饰品
    public static final RegistryObject<Item> DUVACEIN = ITEMS.register(
            "duvacein",
            () -> new Duvacein(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //王老烤
    public static final RegistryObject<Item> DOOF_GNAWOAL = ITEMS.register("doof_gnawoal",
            () -> new DoofGnawoalItem() {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(s -> s.withColor(0x55FFFF));
                }
            }
    );
    //m3饰品
    public static final RegistryObject<Item> XIN_TIRIS = ITEMS.register(
            "xin_tiris",
            () -> new XIN_TIRIS(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //m3的鹅卵石
    public static final RegistryObject<Item> PEBBLE = ITEMS.register("pebble",
            () -> new PebbleItem(new Item.Properties())
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //大理石碎片
    public static final RegistryObject<Item> MARBLESHARD = ITEMS.register(
            "marbleshard",
            () -> new MarbleShardItem(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //haozi饰品
    public static final RegistryObject<Item> HAOZI = ITEMS.register(
            "haozi",
            () -> new haozi(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //颜文字饰品
    public static final RegistryObject<Item> YANWENZI = ITEMS.register(
            "yanwenzi",
            () -> new YanWenZi(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //颜文字的镰刀
    public static final RegistryObject<Item> ZHAOHUANSHILIANDAO = ITEMS.register(
            "zhaohuanshiliandao",
            () -> new SummonerScythe(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //铸币老王的紫昙花
    public static final RegistryObject<Item> ZITANHUA = ITEMS.register(
            "zitanhua",
            () -> new VoidPiercer(new Item.Properties().stacksTo(1).defaultDurability(5152))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //wu1wu2
    public static final RegistryObject<Item> WU1WU2 = ITEMS.register(
            "wu1wu2",
            () -> new wu1wu2(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //wu1wu2压榨商人
    public static final RegistryObject<Item> BLADEOFTRADE = ITEMS.register(
            "bladeoftrade",
            () -> new Wu1wu2Sword()
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //白鲨糖
    public static final RegistryObject<Item> BAISHATANG = ITEMS.register(
            "baishatang",
            () -> new SweetToothRelicItem(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //糖球
    public static final RegistryObject<Item> SUGARBALL = ITEMS.register(
            "sugarball",
            () -> new SugarBallItem(new Item.Properties().stacksTo(64))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    return Component.translatable(this.getDescriptionId(pStack))
                            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FFFF))); // 淡蓝色 (Aqua)
                }
            }
    );
    //菈
    public static final RegistryObject<Item> MIKULA = ITEMS.register(
            "mikula",
            () -> new MikuLa(new Item.Properties().stacksTo(1)) {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    // 1. 获取翻译后的原始名称字符串
                    String rawName = Component.translatable(this.getDescriptionId(pStack)).getString();
                    // 2. 创建承载彩色名字的容器
                    MutableComponent finalName = Component.empty();
                    // 3. 循环处理每个字符
                    for (int i = 0; i < rawName.length(); i++) {
                        // 交替颜色：偶数为白色，奇数为淡紫色（粉色）
                        ChatFormatting color = (i % 2 == 0) ? ChatFormatting.WHITE : ChatFormatting.LIGHT_PURPLE;
                        finalName.append(Component.literal(String.valueOf(rawName.charAt(i))).withStyle(color));
                    }
                    return finalName;
                }
            }
    );
    //菈的祝福
    public static final RegistryObject<Item> MIKUPROP = ITEMS.register(
            "mikuprop",
            () -> new MikuLaMeowBlessingItem(new Item.Properties().stacksTo(1))
            {
                @Override
                public @NotNull Component getName(@NotNull ItemStack pStack) {
                    // 1. 获取翻译后的原始名称字符串
                    String rawName = Component.translatable(this.getDescriptionId(pStack)).getString();
                    // 2. 创建承载彩色名字的容器
                    MutableComponent finalName = Component.empty();
                    // 3. 循环处理每个字符
                    for (int i = 0; i < rawName.length(); i++) {
                        // 交替颜色：偶数为白色，奇数为淡紫色（粉色）
                        ChatFormatting color = (i % 2 == 0) ? ChatFormatting.WHITE : ChatFormatting.LIGHT_PURPLE;
                        finalName.append(Component.literal(String.valueOf(rawName.charAt(i))).withStyle(color));
                    }
                    return finalName;
                }
            }
    );
    //Sim
    public static final RegistryObject<Item> SIM = ITEMS.register(
            "sim",
            () -> new Sim(new Item.Properties().stacksTo(1))
    );




    // 辅助方法：将注册器绑定到 Mod 事件总线
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}