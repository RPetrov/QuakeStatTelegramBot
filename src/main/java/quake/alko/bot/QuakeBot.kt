package quake.alko.bot

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.TableUtils
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.request.SendMessage
import quake.alko.bot.dto.Game
import quake.alko.bot.dto.QuakeFile
import quake.alko.bot.dto.Score
import quake.alko.bot.parsers.LogParser
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


class QuakeBot(val bot: TelegramBot, val config: Config) {

    val databaseUrl = "jdbc:sqlite:/home/roman/quake.db"

    val connectionSource: ConnectionSource = JdbcConnectionSource(databaseUrl)

    val quakeFileDao: Dao<QuakeFile, String> = DaoManager.createDao(connectionSource, QuakeFile::class.java)
    val gameDao: Dao<Game, Int> = DaoManager.createDao(connectionSource, Game::class.java)
    val scoreDao: Dao<Score, Int> = DaoManager.createDao(connectionSource, Score::class.java)

    init {
        TableUtils.createTableIfNotExists(connectionSource, Score::class.java)
        TableUtils.createTableIfNotExists(connectionSource, Game::class.java)
        TableUtils.createTableIfNotExists(connectionSource, QuakeFile::class.java)
    }

    fun start() {
        bot.setUpdatesListener { updates: List<Update?>? ->
            updates?.let {
                try {
                    checkUpdates(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    updates.firstOrNull()?.message()?.chat()?.id()?.let {
                        bot.execute(SendMessage(it, e.message ?: "Ошибка"))
                    }
                }
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    fun checkUpdates(updates: List<Update?>) {
        val updates = updates.filterNotNull()

        for (update in updates) {

            if (update.message().chat().id() !in config.allowedChatIds) {
                bot.execute(SendMessage(update.message().chat().id(), "Нет доступа!"))
                return
            }

            val document = update.message()?.document()
            if (document != null) {
                val request = GetFile(document.fileId())
                val getFileResponse = bot.execute(request)
                val file = getFileResponse.file() // com.pengrad.telegrambot.model.File
                var data = bot.getFileContent(file)

                val quakeFile = LogParser.parse(ByteArrayInputStream(data))
                quakeFile.hash = SHAsum(data)

                if (quakeFileDao.idExists(quakeFile.hash)) {
                    bot.execute(SendMessage(update.message().chat().id(), "Такой файл уже обрабатывался"))
                    return
                }

                saveToDataBase(quakeFile)
                sendStat(update.message().chat().id(), quakeFile.games.toList())
            }

            if (update.message()?.text() == "/stat") {
                gameDao.queryBuilder().query().apply {
                    sendStat(update.message().chat().id(), this)
                }
            }
        }
    }

    fun sendStat(chatId: Long, games: List<Game>) {
        var absMin: Score? = null
        val leaders: MutableMap<String, Int> = mutableMapOf()
        games.forEach {
            val sorted = it.scores.sortedByDescending { score -> score.score }
            sorted.first().apply {
                leaders[this.name] = if (leaders[this.name] == null) 1 else (leaders[this.name]!! + 1)
            }
            if (sorted.last().score < 0 && (absMin == null || absMin!!.score > sorted.last().score)) {
                absMin = sorted.last()
            }
        }
        var message = "Результат дня:\n"
        leaders.entries.sortedByDescending { mutableEntry -> mutableEntry.value }.forEach {
            message += it.key + ": " + it.value + "\n"
        }
        if (absMin != null) {
            message += "\nХудший результат:\n${absMin!!.name}: ${absMin!!.score}"
        }
        bot.execute(SendMessage(chatId, message))
    }

    fun saveToDataBase(quakeFile: QuakeFile) {
        quakeFile.games.forEach {
            it.scores.forEach {
                scoreDao.createIfNotExists(it)
            }
            gameDao.createIfNotExists(it)
        }
        quakeFileDao.createIfNotExists(quakeFile)
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun SHAsum(convertme: ByteArray?): String {
        val md = MessageDigest.getInstance("SHA-1")
        return byteArray2Hex(md.digest(convertme))
    }

    private fun byteArray2Hex(hash: ByteArray): String {
        val formatter = Formatter()
        for (b in hash) {
            formatter.format("%02x", b)
        }
        return formatter.toString()
    }
}

