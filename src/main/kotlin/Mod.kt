package com.algorithmlx.litecorps

//? if neoforge {
/*import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.common.Mod
*///?}
import com.algorithmlx.litecorps.utils.initEvents
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@JvmField
val LOGGER: Logger = LoggerFactory.getLogger("LiteCorps")
const val ModId: String = "litecorps"

//? if neoforge {
/*@Mod(ModId)
class Mod(bus: IEventBus) {
    init {
        initEvents(bus)
    }
}
*///?} else {
object Mod {
    fun onInitialize() {
        initEvents()
    }

    fun onInitializeClient() = initClient()
}
//?}
