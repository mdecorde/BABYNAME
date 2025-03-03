package fr.hnit.babyname

import android.content.Context
import android.util.SparseArray
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/*
The babyname app is free software: you can redistribute it
and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation,
either version 2 of the License, or (at your option) any
later version.

The babyname app is distributed in the hope that it will be
useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
PURPOSE. See the GNU General Public License for more
details.

You should have received a copy of the GNU General
Public License along with the TXM platform. If not, see
http://www.gnu.org/licenses
*/

class BabyNameDatabase : SparseArray<BabyName>() {
    fun initialize(ctx: Context) {
        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    val databaseFileName = "babynames.csv"
                    val reader =
                        BufferedReader(InputStreamReader(ctx.assets.open(databaseFileName)))
                    var lineNumber = 0
                    while (reader.ready()) {
                        lineNumber += 1
                        val line = reader.readLine()
                        val items = line.split(";".toRegex()).toTypedArray()
                        if (items.size != 3) {
                            AppLogger.e(this, "Failed to parse line in $databaseFileName:$lineNumber: $line")
                            break
                        }

                        val name = items[0]
                        val genres =
                            HashSet(listOf(*items[1].split(",".toRegex()).toTypedArray()))
                        val origins =
                            HashSet(listOf(*items[2].split(",".toRegex()).toTypedArray()))

                        // remove empty entries
                        genres.remove("")
                        origins.remove("")

                        if (name.isNotEmpty()) {
                            val b = BabyName(name, genres, origins)
                            this@BabyNameDatabase.put(b.id, b)
                        } else {
                            AppLogger.e(this, "Empty baby name in $databaseFileName:$lineNumber: $line")
                        }
                    }
                    reader.close()

                    AppLogger.d(this, "Loaded " + this@BabyNameDatabase.size() + " names")
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        thread.start()
    }

    fun getAllOrigins(): HashSet<String> {
        val all = HashSet<String>()
        val n = this.size()
        for (i in 0 until n) {
            val entry = this[i]
            if (entry != null) {
                all.addAll(entry.origins)
            }
        }
        return all
    }

    companion object {
        const val GENDER_MALE: String = "m"
        const val GENDER_FEMALE: String = "f"
    }
}
