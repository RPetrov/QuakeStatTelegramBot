package quake.alko.bot

import com.pengrad.telegrambot.TelegramBot

class MyApp {
    companion object {
        @JvmStatic fun main(args: Array<String>) {
            val bot = TelegramBot("5258791305:AAED9zqt0cf53S3MeNdzc9FO-d07_LBcCJA")
            QuakeBot(bot).start()
        }
    }
}