package com.first.themasses.command;

import com.first.themasses.curios.mysterious.KunZhong;
import com.first.themasses.events.prop.BlessingEvents;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.DimensionArgument;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

public class MirrorTestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // 1. 伤害指令
        dispatcher.register(Commands.literal("hurt")
                .requires(source -> source.hasPermission(4))
                .then(Commands.argument("targets", EntityArgument.players())
                        .then(Commands.argument("damage", FloatArgumentType.floatArg(0.0F))
                                .executes(context -> {
                                    float amount = FloatArgumentType.getFloat(context, "damage");

                                    EntityArgument.getPlayers(context, "targets").forEach(target -> {
                                        DamageSource source = target.damageSources().generic();
                                        target.hurt(source, amount);
                                    });

                                    context.getSource().sendSuccess(() -> Component.literal("§c[战斗] 已造成伤害（受防御减免）"), true);
                                    return 1;
                                }))));
        // 2. 坤钟时长修改指令 /hour <小时数> (支持正负数)
        dispatcher.register(Commands.literal("hour")
                .requires(source -> source.hasPermission(4))
                // 1. 修改最小值限制，使用 Integer.MIN_VALUE 或者一个合理的负数
                .then(Commands.argument("amount", IntegerArgumentType.integer())
                        .executes(context -> {
                            int hoursToModify = IntegerArgumentType.getInteger(context, "amount");
                            ServerPlayer player = context.getSource().getPlayerOrException();

                            return CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> stack.getItem() instanceof KunZhong)
                                    .map(slotResult -> {
                                        CompoundTag nbt = slotResult.stack().getOrCreateTag();

                                        int currentTicks = nbt.getInt("EquippedTicks");
                                        // 计算并限制最小值，一行搞定，不会报错
                                        int finalTicks = (int) Math.max(0, currentTicks + (long) hoursToModify * 72000);

                                        nbt.putInt("EquippedTicks", finalTicks);

                                        String action = hoursToModify >= 0 ? "增加" : "减少";
                                        context.getSource().sendSuccess(() -> Component.literal(
                                                "§6[坤钟] 已" + action + " " + Math.abs(hoursToModify) + " 小时时长 (总计: " + finalTicks/72000 + " 小时)"
                                        ), true);

                                        return 1;
                                    }).orElseGet(() -> {
                                        context.getSource().sendFailure(Component.literal("§c[错误] 你必须佩戴坤钟饰品才能使用此指令"));
                                        return 0;
                                    });
                        })));
    }
}