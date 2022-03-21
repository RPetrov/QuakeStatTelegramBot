package quake.alko.bot

import java.io.File
import java.util.*

/**
 * Config for bit
 */
class Config {

    companion object {
        private const val BOT_TOKEN = "bot_token"
        private const val WHITE_LIST = "white_list"
    }

    /**
     * Token to access to Telegram Bot
     */
    lateinit var token: String
        private set

    /**
     * Allowed Chat Ids
     */
    lateinit var allowedChatIds: List<Long>
        private set

    fun initConfig(pathToConfigFile: String) {
        val properties = Properties()
        properties.load(File(pathToConfigFile).inputStream())
        token = properties.getProperty(BOT_TOKEN)
        allowedChatIds = properties.getProperty(WHITE_LIST)?.split(",")?.map { s -> s.toLong() } ?: emptyList()
    }
}