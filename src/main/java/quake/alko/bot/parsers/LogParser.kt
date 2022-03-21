package quake.alko.bot.parsers

import quake.alko.bot.dto.QuakeFile
import quake.alko.bot.dto.Score
import quake.alko.bot.dto.Game
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*

class LogParser {

    companion object {
        fun parse(inputStream: InputStream): QuakeFile {

            val quakeFile = QuakeFile().apply {
                date = Date()
            }

            val br = BufferedReader(InputStreamReader(inputStream))
            val allText = br.use(BufferedReader::readLines)

            var game: Game? = null
            for (line in allText) {
                if (line.contains("InitGame")) {
                    if (game != null) {
                        System.out.println("Какая-то ошибка!")
                    }
                    val data = line.split("\\")
                    for (i in 0..data.size - 1) {
                        if (data[i] == "mapname")
                            game = Game().apply {
                                mapName = data[i + 1]
                            }
                    }
                } else if (game != null && line.contains("score")) {
                    val data = line.split(" ")
                    val score = Score().apply {
                        name = data[10]
                        ping = data[6].toInt()
                        score = data[3].toInt()
                    }
                    score.game = game
                    (game.scores as ArrayList).add(score)
                } else if (line.contains("ShutdownGame")) {
                    if (game != null && game.scores.isNotEmpty()) {
                        game.quakeFile = quakeFile
                        (quakeFile.games as ArrayList).add(game)
                    } else if (game != null && game.scores.isEmpty()) {
                        // bot.execute(SendMessage(update.message().chat().id(), "Зафиксирована игра без результата")) todo
                    }
                    game = null
                }
            }

            return quakeFile
        }
    }
}