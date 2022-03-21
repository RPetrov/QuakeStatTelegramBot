package quake.alko.bot

import com.pengrad.telegrambot.TelegramBot

class Runner {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val config = Config().apply {
                initConfig(args[0])
            }

            val bot = TelegramBot(config.token)
            QuakeBot(bot, config).start()
        }
    }
}