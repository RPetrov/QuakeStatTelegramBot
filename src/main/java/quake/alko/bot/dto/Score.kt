package quake.alko.bot.dto

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

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
