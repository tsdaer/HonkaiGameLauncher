package screen

import cafe.adriel.voyager.core.model.ScreenModel
import com.russhwolf.settings.Settings

class HomeScreenModel(
    val settings: Settings = Settings(),
) : ScreenModel {

    override fun onDispose() {
        super.onDispose()
    }
}