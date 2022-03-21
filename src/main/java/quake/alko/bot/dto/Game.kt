package quake.alko.bot.dto

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

@DatabaseTable(tableName = "games")
data class Game(@DatabaseField() var mapName: String, @ForeignCollectionField val scores: Collection<Score> = ArrayList()) {

    constructor() : this("N/A")

    @DatabaseField(id = true)
    private val id: String = UUID.randomUUID().toString()

    @DatabaseField(foreign = true)
    var quakeFile: QuakeFile? = null
}
