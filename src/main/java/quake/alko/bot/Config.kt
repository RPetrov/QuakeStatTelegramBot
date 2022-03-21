package quake.alko.bot

import java.io.File
import java.util.*

/**
 * Config for bit
 */
class Config {

    companion object {
        private const val BOT_TOKEN = "bot_token"
    }

    /**
     * Token to access to Telegram Bot
     */
    lateinit var token: String
        private set

    fun initConfig(pathToConfigFile: String) {
        val properties = Properties()
        properties.load(File(pathToConfigFile).inputStream())
        token = properties.getProperty(BOT_TOKEN)
    }
}