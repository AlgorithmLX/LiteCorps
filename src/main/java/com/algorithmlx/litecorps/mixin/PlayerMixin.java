package com.algorithmlx.litecorps.mixin;

//? if fabricLike
import com.algorithmlx.litecorps.api.DeathChestGatherEvent;
import com.algorithmlx.litecorps.utils.DeathChestState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow
    public abstract Inventory getInventory();

    @Inject(method = "dropEquipment", at = @At("HEAD"), cancellable = true)
    //$ if >1.21.1 'private void dropEquipment(ServerLevel level, CallbackInfo ci) {' else 'private void dropEquipment(CallbackInfo ci) {'
    private void dropEquipment(ServerLevel level, CallbackInfo ci) {
        var player = (ServerPlayer) (Object) this;
        var inv = this.getInventory();
        //? if <1.21.11
        //var level = player.serverLevel();

        List<ItemStack> armor = new ArrayList<>();
        boolean hasArmor = false;
        for (int i = 36; i <= 39; i++) {
            var stack = inv.getItem(i).copy();
            armor.add(stack);
            if (!stack.isEmpty()) hasArmor = true;
            inv.setItem(i, ItemStack.EMPTY);
        }

        var offhand = inv.getItem(40).copy();
        boolean hasOffhand = !offhand.isEmpty();
        inv.setItem(40, ItemStack.EMPTY);

        List<ItemStack> hotbar = new ArrayList<>();
        boolean hasHotbar = false;
        for (int i = 0; i < 9; i++) {
            var stack = inv.getItem(i).copy();
            hotbar.add(stack);
            if (!stack.isEmpty()) hasHotbar = true;
            inv.setItem(i, ItemStack.EMPTY);
        }

        List<ItemStack> mainInv = new ArrayList<>();
        boolean hasMainInv = false;
        for (int i = 9; i < 36; i++) {
            var stack = inv.getItem(i).copy();
            mainInv.add(stack);
            if (!stack.isEmpty()) hasMainInv = true;
            inv.setItem(i, ItemStack.EMPTY);
        }

        List<ItemStack> extraItems = new ArrayList<>();
        for (int i = 41; i < inv.getContainerSize(); i++) {
            var stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                extraItems.add(stack.copy());
                inv.setItem(i, ItemStack.EMPTY);
            }
        }

        //? if fabricLike
        DeathChestGatherEvent.EVENT.invoker().onGather(player, extraItems);

        boolean hasBaseItems = hasArmor || hasOffhand || hasHotbar || hasMainInv;
        if (!hasBaseItems && extraItems.isEmpty()) {
            return;
        }

        Map<Integer, ItemStack> baseChestItems = new HashMap<>();
        boolean baseIsDouble;

        if (hasMainInv && (hasArmor || hasOffhand || hasHotbar)) {
            baseIsDouble = true;
            for (int i = 0; i < 4; i++) if (!armor.get(i).isEmpty()) baseChestItems.put(i, armor.get(i));
            if (hasOffhand) baseChestItems.put(4, offhand);
            for (int i = 0; i < 9; i++) if (!hotbar.get(i).isEmpty()) baseChestItems.put(i + 9, hotbar.get(i));
            for (int i = 0; i < 27; i++) if (!mainInv.get(i).isEmpty()) baseChestItems.put(i + 18, mainInv.get(i));
        } else if (hasMainInv) {
            baseIsDouble = false;
            for (int i = 0; i < 27; i++) if (!mainInv.get(i).isEmpty()) baseChestItems.put(i, mainInv.get(i));
        } else {
            baseIsDouble = false;
            for (int i = 0; i < 4; i++) if (!armor.get(i).isEmpty()) baseChestItems.put(i, armor.get(i));
            if (hasOffhand) baseChestItems.put(4, offhand);
            for (int i = 0; i < 9; i++) if (!hotbar.get(i).isEmpty()) baseChestItems.put(i + 9, hotbar.get(i));
        }

        var source = player.getLastDamageSource();
        var basePos = player.blockPosition();

        if (source != null && source.is(DamageTypes.IN_WALL)) {
            while (
                    basePos.getY() <
                            //$ if >1.21.1 'level.getMaxY()' else 'level.getMaxBuildHeight()'
                            level.getMaxY()
                            && !level.isEmptyBlock(basePos)) {
                basePos = basePos.above();
            }
        }

        if (basePos.getY() <
                //$ if >1.21.1 'level.getMaxY()' else 'level.getMaxBuildHeight()'
                level.getMaxY()
        ) {
            basePos = litecorps$findNearestIsland(level, basePos);
        }

        basePos = litecorps$getSafeChestPos(level, basePos, baseIsDouble);

        for (BlockPos p : BlockPos.betweenClosed(basePos.offset(-2, -1, -2), basePos.offset(2, 2, 2))) {
            if (level.getBlockState(p).is(Blocks.FIRE) || level.getBlockState(p).is(Blocks.SOUL_FIRE)) {
                level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
            }
        }

        if (level.getBlockState(basePos).is(Blocks.LAVA) || level.getBlockState(basePos.below()).is(Blocks.LAVA)) {
            while ((level.getBlockState(basePos).is(Blocks.LAVA) || level.getBlockState(basePos.below()).is(Blocks.LAVA)) && basePos.getY() <
                    //$ if >1.21.1 'level.getMaxY()' else 'level.getMaxBuildHeight()'
                    level.getMaxY()
            ) {
                basePos = basePos.above();
            }

            basePos = litecorps$getSafeChestPos(level, basePos, baseIsDouble);

            var platformBlock = Blocks.COBBLESTONE;
            if (level.dimension() == Level.END) {
                platformBlock = Blocks.END_STONE;
            } else if (level.dimension() == Level.NETHER) {
                platformBlock = Blocks.BASALT;
            }

            var pos2 = basePos.east();
            BlockPos[] platformPositions = {
                    basePos.below(), pos2.below(),
                    basePos.north(), pos2.north(),
                    basePos.south(), pos2.south(),
                    basePos.west(), pos2.east()
            };
            for (BlockPos p : platformPositions) {
                level.setBlockAndUpdate(p, platformBlock.defaultBlockState());
            }
        }

        if (hasBaseItems) {
            if (!baseIsDouble) {
                level.setBlockAndUpdate(basePos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE));
                DeathChestState.get(level).addChest(basePos, basePos, player.getUUID());

                Container chestInv = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, level.getBlockState(basePos), level, basePos, true);
                if (chestInv != null) {
                    for (Map.Entry<Integer, ItemStack> entry : baseChestItems.entrySet()) {
                        chestInv.setItem(entry.getKey(), entry.getValue());
                    }
                }
            } else {
                var rightPos = basePos.east();
                level.setBlockAndUpdate(basePos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
                level.setBlockAndUpdate(rightPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
                DeathChestState.get(level).addChest(basePos, rightPos, player.getUUID());

                Container chestInv = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, level.getBlockState(basePos), level, basePos, true);
                if (chestInv != null) {
                    for (Map.Entry<Integer, ItemStack> entry : baseChestItems.entrySet()) {
                        chestInv.setItem(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        var currentPos = hasBaseItems ? basePos.above() : basePos;
        int extraIndex = 0;

        while (extraIndex < extraItems.size() && currentPos.getY() <=
                //$ if >1.21.1 'level.getMaxY()' else 'level.getMaxBuildHeight()'
                level.getMaxY()
        ) {
            int remaining = extraItems.size() - extraIndex;
            boolean needDouble = remaining > 27;

            currentPos = litecorps$getSafeChestPos(level, currentPos, needDouble);

            if (!needDouble) {
                level.setBlockAndUpdate(currentPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.SINGLE));
                DeathChestState.get(level).addChest(currentPos, currentPos, player.getUUID());

                Container chestInv = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, level.getBlockState(currentPos), level, currentPos, true);
                if (chestInv != null) {
                    for (int slot = 0; slot < 27 && extraIndex < extraItems.size(); slot++) {
                        chestInv.setItem(slot, extraItems.get(extraIndex++));
                    }
                }
            } else {
                var rightPos = currentPos.east();
                level.setBlockAndUpdate(currentPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.LEFT));
                level.setBlockAndUpdate(rightPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.NORTH).setValue(ChestBlock.TYPE, ChestType.RIGHT));
                DeathChestState.get(level).addChest(currentPos, rightPos, player.getUUID());

                Container chestInv = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, level.getBlockState(currentPos), level, currentPos, true);
                if (chestInv != null) {
                    for (int slot = 0; slot < 54 && extraIndex < extraItems.size(); slot++) {
                        chestInv.setItem(slot, extraItems.get(extraIndex++));
                    }
                }
            }
            currentPos = currentPos.above();
        }

        while (extraIndex < extraItems.size()) {
            player.drop(extraItems.get(extraIndex++), true);
        }

        DeathChestState.get(level).addPending(player.getUUID(), basePos);
        ci.cancel();
    }

    @Unique
    private BlockPos litecorps$findNearestIsland(ServerLevel level, BlockPos center) {
        var radius = 64;

        for (int d = 0; d <= radius; d++) {
            for (int x = -d; x <= d; x++) {
                for (int z = -d; z <= d; z++) {
                    if (Math.abs(x) == d || Math.abs(z) == d) {
                        var topPos = level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, new BlockPos(center.getX() + x, 0, center.getZ() + z));
                        if (topPos.getY() >
                                //$ if >1.21.1 'level.getMaxY()' else 'level.getMaxBuildHeight()'
                                level.getMaxY()
                        ) {
                            return topPos;
                        }
                    }
                }
            }
        }

        return new BlockPos(center.getX(), radius, center.getZ());
    }

    @Unique
    private BlockPos litecorps$getSafeChestPos(ServerLevel level, BlockPos center, boolean needDouble) {
        var maxRadius = 5;

        for (int r = 0; r <= maxRadius; r++) {
            for (int y = 0; y <= r; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        if (Math.max(y, Math.max(Math.abs(x), Math.abs(z))) == r) {
                            var checkPos = center.offset(x, y, z);
                            var isSafe = !level.getBlockState(checkPos).is(Blocks.CHEST);

                            if (needDouble) {
                                isSafe = isSafe && !level.getBlockState(checkPos.east()).is(Blocks.CHEST);
                            }

                            if (isSafe && checkPos.getY() <=
                                    //$ if >1.21.1 'level.getMaxY()' else 'level.getMaxBuildHeight()'
                                    level.getMaxY()
                            ) {
                                return checkPos;
                            }
                        }
                    }
                }
            }
        }

        return center;
    }
}
