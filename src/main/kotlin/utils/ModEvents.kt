package com.algorithmlx.litecorps.utils

import com.algorithmlx.litecorps.ModId
//? if fabricLike {
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.fabricmc.fabric.api.event.player.UseBlockCallback
import net.minecraft.world.phys.HitResult
//?} else {
/*import net.minecraft.world.level.Level
import net.neoforged.bus.api.IEventBus
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.BlockEvent
*///?}
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.state.properties.ChestType

private fun removeDeathNote(player: Player) {
    val inv = player.inventory
    for (i in 0 until inv.containerSize) {
        val stack = inv.getItem(i)
        if (stack.`is`(Items.PAPER)) {
            val customData = stack.get(DataComponents.CUSTOM_DATA)
            if (customData != null && customData.copyTag().contains("${ModId}_RespawnCoordsPaper")) {
                inv.setItem(i, ItemStack.EMPTY)
            }
        }
    }
}

//? if fabricLike {
fun initEvents() {
    UseBlockCallback.EVENT.register { player, level, _, hitResult ->
        if (!level.isClientSide && hitResult.type == HitResult.Type.BLOCK) {
            val pos = hitResult.blockPos
            if (level.getBlockState(pos).`is`(Blocks.CHEST)) {
                val owner = DeathChestState.get(level).getOwner(pos)
                if (owner != null) {
                    if (owner != player.uuid) {
                        //? if <=1.21.11 {
                        player.displayClientMessage(Component.translatable("subtitles.block.chest.locked"), true)
                        //?} else
                        //player.sendOverlayMessage(Component.translatable("subtitles.block.chest.locked"))
                        return@register InteractionResult.FAIL
                    } else {
                        removeDeathNote(player)
                    }
                }
            }
        }

        return@register InteractionResult.PASS
    }

    ServerPlayerEvents.AFTER_RESPAWN.register { _, new, _ ->
        val chestPos = DeathChestState.get(new.level()).consume(new.uuid)
        if (chestPos != null) {
            val paper = ItemStack(Items.PAPER)
            paper.set(DataComponents.LORE, ItemLore(listOf(
                Component.literal("Pos: X: ${chestPos.x} Y: ${chestPos.y} Z: ${chestPos.z}")
            )))

            paper.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply {
                this.putBoolean("${ModId}_RespawnCoordsPaper", true)
            }))

            paper.set(DataComponents.ITEM_NAME, Component.literal("Death Note"))

            val itemEntity = ItemEntity(new.level(), new.x, new.y, new.z, paper).apply {
                this.setTarget(new.uuid)
            }

            new.level().addFreshEntity(itemEntity)
        }
    }

    PlayerBlockBreakEvents.BEFORE.register { level, player, pos, state, _ ->
        if (!level.isClientSide && state.`is`(Blocks.CHEST)) {
            val owner = DeathChestState.get(level).getOwner(pos)
            if (owner != null) {
                if (owner != player.uuid) {
                    //? if <=1.21.11 {
                    player.displayClientMessage(Component.translatable("subtitles.block.chest.locked"), true)
                    //?} else
                    //player.sendOverlayMessage(Component.translatable("subtitles.block.chest.locked"))
                    return@register false
                }

                removeDeathNote(player)

                var otherPos: BlockPos? = null
                if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
                    otherPos = pos.relative(ChestBlock.getConnectedDirection(state))

                val container = ChestBlock.getContainer(Blocks.CHEST as ChestBlock, state, level, pos, true)
                if (container != null) {
                    Containers.dropContents(level, pos, container)
                    container.clearContent()
                }

                val chestState = DeathChestState.get(level)
                chestState.removeChest(pos)
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())

                if (otherPos != null && level.getBlockState(otherPos).`is`(Blocks.CHEST)) {
                    chestState.removeChest(otherPos)
                    level.setBlockAndUpdate(otherPos, Blocks.AIR.defaultBlockState())
                }

                return@register false
            }
        }

        true
    }
}
//?} else {
/*fun initEvents(bus: IEventBus) {
    bus.addListener<PlayerInteractEvent.RightClickBlock> {
        val player = it.entity
        val level = it.level
        val pos = it.pos

        if (!level.isClientSide) {
            val owner = DeathChestState.get(level).getOwner(pos)
            if (owner != null) {
                if (owner != player.uuid) {
                    //? if <=1.21.11 {
                    */
/*player.displayClientMessage(Component.translatable("subtitles.block.chest.locked"), true)
                    *//*
//?} else
                    player.sendOverlayMessage(Component.translatable("subtitles.block.chest.locked"))
                    it.cancellationResult = InteractionResult.FAIL
                } else {
                    removeDeathNote(player)
                }
            }
        }
    }

    bus.addListener<PlayerEvent.Clone> {
        val new = it.entity
        val chestPos = DeathChestState.get(new.level()).consume(new.uuid)
        if (chestPos != null) {
            val paper = ItemStack(Items.PAPER)
            paper.set(DataComponents.LORE, ItemLore(listOf(
                Component.literal("Pos: X: ${chestPos.x} Y: ${chestPos.y} Z: ${chestPos.z}")
            )))

            paper.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply {
                this.putBoolean("${ModId}_RespawnCoordsPaper", true)
            }))

            paper.set(DataComponents.ITEM_NAME, Component.literal("Death Note"))

            val itemEntity = ItemEntity(new.level(), new.x, new.y, new.z, paper).apply {
                this.target = new.uuid
            }

            new.level().addFreshEntity(itemEntity)
        }
    }

    bus.addListener<BlockEvent.BreakEvent> {
        val level = it.level as Level
        val pos = it.pos
        val state = it.state
        val player = it.player
        if (!level.isClientSide && state.`is`(Blocks.CHEST)) {
            val owner = DeathChestState.get(level).getOwner(pos)
            if (owner != null) {
                if (owner != player.uuid) {
                    //? if <=1.21.11 {
                    */
/*player.displayClientMessage(Component.translatable("subtitles.block.chest.locked"), true)
                    *//*
//?} else
                    player.sendOverlayMessage(Component.translatable("subtitles.block.chest.locked"))
                    it.isCanceled = true
                }

                removeDeathNote(player)

                var otherPos: BlockPos? = null
                if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
                    otherPos = pos.relative(ChestBlock.getConnectedDirection(state))

                val container = ChestBlock.getContainer(Blocks.CHEST as ChestBlock, state, level, pos, true)
                if (container != null) {
                    Containers.dropContents(level, pos, container)
                    container.clearContent()
                }

                val chestState = DeathChestState.get(level)
                chestState.removeChest(pos)
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState())

                if (otherPos != null && level.getBlockState(otherPos).`is`(Blocks.CHEST)) {
                    chestState.removeChest(otherPos)
                    level.setBlockAndUpdate(otherPos, Blocks.AIR.defaultBlockState())
                }

                it.isCanceled = true
            }
        }
    }
}
*///?}
