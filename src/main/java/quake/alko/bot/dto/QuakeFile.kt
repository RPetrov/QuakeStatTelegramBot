package quake.alko.bot.dto

import com.j256.ormlite.field.DatabaseField
import com.j256.ormlite.field.ForeignCollectionField
import com.j256.ormlite.table.DatabaseTable
import java.util.*

@DatabaseTable(tableName = "files")
data class QuakeFile(
        @DatabaseField(id = true) var hash: String,

        @DatabaseField()
        var date: Date,

        @ForeignCollectionField val games: Collection<Game> = ArrayList()) {

    constructor() : this("N/A", Date())
}
