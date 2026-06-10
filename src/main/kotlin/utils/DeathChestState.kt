package com.algorithmlx.litecorps.utils

import com.algorithmlx.litecorps.ModId
import net.minecraft.core.BlockPos
//? if <1.21.11 {
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
//?} else {
/*import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.core.UUIDUtil
import net.minecraft.world.level.saveddata.SavedDataType
import java.util.stream.Collectors
*///?}
import net.minecraft.util.datafix.DataFixTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.saveddata.SavedData
import java.util.UUID

//$ if >1.21.1 'class DeathChestState(entries: List<ChestEntry>, pendingEntries: List<PendingEntry>): SavedData() {' else 'class DeathChestState: SavedData() {'
class DeathChestState: SavedData() {
    private val chests = hashMapOf<BlockPos, UUID>()
    private val pending = hashMapOf<UUID, BlockPos>()

    fun addChest(pos: BlockPos, pos2: BlockPos, owner: UUID) {
        chests[pos] = owner
        chests[pos2] = owner
        setDirty()
    }

    fun addPending(player: UUID, pos: BlockPos) {
        pending[player] = pos.immutable()
        this.setDirty()
    }

    fun consume(player: UUID): BlockPos? {
        val pos = pending.remove(player)
        if (pos != null) this.setDirty()
        return pos
    }

    fun getOwner(pos: BlockPos): UUID? = chests[pos]

    fun removeChest(pos: BlockPos) {
        if (chests.remove(pos.immutable()) != null)
            this.setDirty()
    }

    //? if >=1.21.11 {
    /*constructor(): this(listOf(), listOf())
    init {
        entries.forEach { (pos, owner) -> this.chests[pos] = owner }
        pendingEntries.forEach { (pos, owner) -> this.pending[owner] = pos }
    }


    data class ChestEntry(val pos: BlockPos, val owner: UUID) {
        companion object {
            @JvmField
            val CODEC = RecordCodecBuilder.create { instance ->
                instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(ChestEntry::pos),
                    UUIDUtil.CODEC.fieldOf("owner").forGetter(ChestEntry::owner)
                ).apply(instance, ::ChestEntry)
            }
        }
    }

    data class PendingEntry(val pos: BlockPos, val owner: UUID) {
        companion object {
            @JvmField
            val CODEC = RecordCodecBuilder.create { instance ->
                instance.group(
                    BlockPos.CODEC.fieldOf("pos").forGetter(PendingEntry::pos),
                    UUIDUtil.CODEC.fieldOf("player").forGetter(PendingEntry::owner)
                ).apply(instance, ::PendingEntry)
            }
        }
    }


    fun getEntries(): List<ChestEntry> = chests.entries.stream()
        .map { ChestEntry(it.key, it.value) }
        .collect(Collectors.toList())

    fun getPendingEntries(): List<PendingEntry> = chests.entries.stream()
        .map { PendingEntry(it.key, it.value) }
        .collect(Collectors.toList())
     *///?}

    //? if <1.21.11 {
    override fun save(
        tag: CompoundTag,
        provider: HolderLookup.Provider
    ): CompoundTag {
        val list = ListTag()
        chests.forEach { (pos, uuid) ->
            val compound = CompoundTag()
            compound.putInt("X", pos.x)
            compound.putInt("Y", pos.y)
            compound.putInt("Z", pos.z)
            compound.putUUID("Owner", uuid)
            list.add(compound)
        }

        tag.put("DeathChests", list)

        return tag
    }
    //?}

    companion object {
        //? if <1.21.11 {
        @JvmStatic
        private fun load(tag: CompoundTag): DeathChestState {
            val state = DeathChestState()
            val list = tag.getList("DeathChests", Tag.TAG_COMPOUND.toInt())

            list.indices.forEach {
                val entry = list.getCompound(it)
                val pos = BlockPos(entry.getInt("X"), entry.getInt("Y"), entry.getInt("Z"))
                val owner = entry.getUUID("Owner")
                state.chests[pos] = owner
            }

            return state
        }

        @JvmStatic
        fun get(level: Level): DeathChestState {
            if (level.isClientSide) throw IllegalArgumentException()
            return (level as ServerLevel).server.overworld().dataStorage
                //$ if >1.21.1 '.computeIfAbsent(TYPE)' else '.computeIfAbsent(Factory(::DeathChestState, { tag, _ -> load(tag) }, DataFixTypes.LEVEL), ModId)'
                .computeIfAbsent(Factory(::DeathChestState, { tag, _ -> load(tag) }, DataFixTypes.LEVEL), ModId)
        }
        //?} else {
        /*@JvmField
        val CODEC = RecordCodecBuilder.create { instance -> instance.group(
            ChestEntry.CODEC.listOf().fieldOf("chests").forGetter(DeathChestState::getEntries),
            PendingEntry.CODEC.listOf().fieldOf("pending").forGetter(DeathChestState::getPendingEntries)
        ).apply(instance, ::DeathChestState) }

        @JvmField
        val TYPE =
            //$ if 1.21.11 'SavedDataType("$ModId:chests", ::DeathChestState, CODEC, DataFixTypes.LEVEL)' else 'SavedDataType(ResLoc.parse("$ModId:chests"), ::DeathChestState, CODEC, DataFixTypes.LEVEL)'
            SavedDataType(ResLoc.parse("$ModId:chests"), ::DeathChestState, CODEC, DataFixTypes.LEVEL)
        *///?}
    }
}
