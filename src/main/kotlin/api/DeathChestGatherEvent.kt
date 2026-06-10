package com.algorithmlx.litecorps.api

//? if fabricLike {
import net.fabricmc.fabric.api.event.EventFactory
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

fun interface DeathChestGatherEvent {
    fun onGather(player: ServerPlayer, items: List<ItemStack>)

    companion object {
        @JvmField
        val EVENT = EventFactory.createArrayBacked(DeathChestGatherEvent::class.java) { listeners ->
            DeathChestGatherEvent { player, items ->
                listeners.forEach {
                    it.onGather(player, items)
                }
            }
        }
    }
}
//?}
