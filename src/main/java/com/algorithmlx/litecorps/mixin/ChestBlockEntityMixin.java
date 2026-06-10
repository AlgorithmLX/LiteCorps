package com.algorithmlx.litecorps.mixin;

import com.algorithmlx.litecorps.utils.DeathChestState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
//? if >1.21.1
/*import net.minecraft.world.entity.ContainerUser;*/
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChestBlockEntity.class)
public class ChestBlockEntityMixin {
    @Inject(method = "stopOpen", at = @At("TAIL"))
    //$ if >1.21.1 'private void stopOpen(ContainerUser containerUser, CallbackInfo ci) {' else 'private void stopOpen(Player player, CallbackInfo ci) {'
    private void stopOpen(Player player, CallbackInfo ci) {
        var chest = (ChestBlockEntity) (Object) this;
        var level = chest.getLevel();

        if (level instanceof ServerLevel && !chest.isRemoved()) {
            var pos = chest.getBlockPos();

            var chestState = DeathChestState.get(level);
            var owner = chestState.getOwner(pos);

            if (owner != null) {
                var state = chest.getBlockState();
                var container = ChestBlock.getContainer((ChestBlock) Blocks.CHEST, state, level, pos, true);

                if (container != null && container.isEmpty()) {
                    BlockPos otherPos = null;
                    if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                        otherPos = pos.relative(ChestBlock.getConnectedDirection(state));
                    }

                    chestState.removeChest(pos);
                    level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());

                    if (otherPos != null && level.getBlockState(otherPos).is(Blocks.CHEST)) {
                        chestState.removeChest(otherPos);
                        level.setBlockAndUpdate(otherPos, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
    }
}
