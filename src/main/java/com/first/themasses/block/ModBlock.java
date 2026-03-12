package com.first.themasses.block;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
//方块注册
public class ModBlock {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, "themasses");

    public static final RegistryObject<Block> SOUL_BLOCK = BLOCKS.register("soul_block",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK) // 地图显示颜色为黑色
                    .strength(15.0f, 6.0f)         // 硬度15（钻石斧约4秒），抗性6
                    .requiresCorrectToolForDrops()  // 必须使用正确工具（斧头）才掉落
                    .lightLevel((state) -> 15)      // 最高亮度15
                    .sound(SoundType.WOOD)     // 灵魂沙踩踏声
            ));
    public static final RegistryObject<Block> AUVIOTRE_BLOCK = BLOCKS.register("laxauviotre",
            () -> new HorizontalDirectionalBlock(BlockBehaviour.Properties.of()
                    .strength(0f) // 瞬掘
                    .noOcclusion()) {

                // 核心逻辑 1：初始化默认朝向
                {
                    this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
                }

                // 核心逻辑 2：放置时检测玩家位置，让方块面对玩家
                @Override
                public BlockState getStateForPlacement(BlockPlaceContext context) {
                    return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
                }

                // 核心逻辑 3：告诉游戏这个方块拥有 FACING 属性
                @Override
                protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
                    builder.add(FACING);
                }
            });
}