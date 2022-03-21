package quake.alko.bot

import com.j256.ormlite.dao.Dao
import com.j256.ormlite.dao.DaoManager
import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.jdbc.JdbcConnectionSource
import com.j256.ormlite.support.ConnectionSource
import com.j256.ormlite.table.DatabaseTable
import com.j256.ormlite.table.TableUtils
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Chat
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.request.GetFile
import com.pengrad.telegrambot.request.SendMessage
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


@DatabaseTable(tableName = "files")
data class QuakeFile(
        @DatabaseField(id = true) var hash: String,

        @DatabaseField()
        var date: Date,

        @ForeignCollectionField val games: Collection<Game> = ArrayList()) {

    constructor() : this("N/A", Date())
}

@DatabaseTable(tableName = "games")
data class Game(@DatabaseField() var mapName: String, @ForeignCollectionField val scores: Collection<Score> = ArrayList()) {

    constructor() : this("N/A")

    @DatabaseField(id = true)
    private val id: String = UUID.randomUUID().toString()

    @DatabaseField(foreign = true)
    var quakeFile: QuakeFile? = null
}

@DatabaseTable(tableName = "scores")
data class Score(
        @DatabaseField()
        var name: String,

        @DatabaseField()
        var ping: Int,

        @DatabaseField()
        var score: Int
) {

    constructor() : this("N/A", 0, 0)

    @DatabaseField(id = true)
    private val id: String = UUID.randomUUID().toString()

    @DatabaseField(foreign = true)
    var game: Game? = null
}

class QuakeBot(val bot: TelegramBot) {

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
            val photo = update.message()?.photo()
            if (photo != null) {
                // bot.execute(SendMessage(update.message().chat().id(), "Фото больше присылать не надо"))
            }

            if(update.message().chat().type() != Chat.Type.supergroup || update.message().chat().title() != "АлкоUnreal"){
                bot.execute(SendMessage(update.message().chat().id(), "Нет доступа!"))
                return
            }

            val document = update.message()?.document()
            if (document != null) {
                val request = GetFile(document.fileId())
                val getFileResponse = bot.execute(request)
                val file = getFileResponse.file() // com.pengrad.telegrambot.model.File
                var data = bot.getFileContent(file)
                val quakeFile = quakeFileDao.createObjectInstance().apply {
                    date = Date()
                    hash = SHAsum(data)
                }
                if(quakeFileDao.idExists(quakeFile.hash)){
                    bot.execute(SendMessage(update.message().chat().id(), "Такой файл уже обрабатывался"))
                    return
                }
                val br = BufferedReader(InputStreamReader(ByteArrayInputStream(data)))
                data = null
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
                                game = gameDao.createObjectInstance().apply {
                                    mapName = data[i + 1]
                                }
                        }
                    } else if (game != null && line.contains("score")) {
                        val data = line.split(" ")
                        val score = scoreDao.createObjectInstance().apply {
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
                            bot.execute(SendMessage(update.message().chat().id(), "Зафиксирована игра без результата"))
                        }
                        game = null
                    }
                }

                saveToDataBase(quakeFile)
                sendStat(update.message().chat().id(), quakeFile.games.toList())
            }

            if(update.message()?.text() == "/stat"){
                gameDao.queryBuilder().query().apply {
                    sendStat(update.message().chat().id(), this)
                }
            }
        }
    }

    fun sendStat(chatId: Long, games: List<Game>){
        var absMin: Score? = null
        val leaders: MutableMap<String, Int> = mutableMapOf()
        games.forEach {
            val sorted = it.scores.sortedByDescending { score -> score.score }
            sorted.first().apply {
                leaders[this.name] = if(leaders[this.name] == null) 1 else (leaders[this.name]!! + 1)
            }
            if(sorted.last().score < 0 && (absMin == null || absMin!!.score > sorted.last().score)){
                absMin = sorted.last()
            }
        }
        var message = "Результат дня:\n"
        leaders.entries.sortedByDescending { mutableEntry -> mutableEntry.value }.forEach {
            message += it.key + ": " + it.value + "\n"
        }
        if(absMin != null){
            message+="\nХудший результат:\n${absMin!!.name}: ${absMin!!.score}"
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
    fun SHAsum(convertme: ByteArray?): String {
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

