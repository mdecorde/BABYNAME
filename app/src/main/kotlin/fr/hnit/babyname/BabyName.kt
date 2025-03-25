package fr.hnit.babyname

import android.content.Context
import java.io.Serializable
import java.util.Locale

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

class BabyName(var name: String, var genres: HashSet<String>, var origins: HashSet<String>) : Serializable {
    var id = nextId++
    var soundex = generateSoundex(name)

    fun getMetaString(context: Context): String {
        val genres = ArrayList(genres)
        val origins = ArrayList(origins)
        genres.sort()
        origins.sort()
        var meta = ""
        if (genres.isNotEmpty()) {
            meta += genresToLocale(context, genres).toString()
        }
        meta += " "
        if (origins.isNotEmpty()) {
            meta += originsToLocale(context, origins).toString()
        }
        return meta
    }

    companion object {
        private var nextId = 0

        private fun genresToLocale(context: Context, origins: ArrayList<String>): ArrayList<String> {
            val ret = ArrayList<String>()
            var i = 0
            while (i < origins.size) {
                when (origins[i]) {
                    BabyNameDatabase.GENDER_FEMALE -> ret.add(context.getString(R.string.girl))
                    BabyNameDatabase.GENDER_MALE -> ret.add(context.getString(R.string.boy))
                    else -> ret.add(origins[i])
                }
                i += 1
            }
            return ret
        }

        // Make every origin begin with an upper case letter.
        // It would be nice to have proper localisation in the future.
        private fun originsToLocale(context: Context, origins: ArrayList<String>): ArrayList<String> {
            val ret = ArrayList<String>()
            var i = 0
            while (i < origins.size) {
                val origin = origins[i]
                ret.add(origin.substring(0, 1).uppercase(Locale.getDefault()) + origin.substring(1))
                i += 1
            }
            return ret
        }

        // Return Soundex identifier encoded as Integer.
        private fun generateSoundex(name: String): Int {
            var prev = 0
            var ex = 0
            for (i in name.indices) {
                val c = name[i]
                val x = when (c) {
                    'a', 'e', 'i', 'o', 'u', 'y', 'h', 'w' -> 1
                    'b', 'f', 'p', 'v' -> 2
                    'c', 'g', 'j', 'k', 'q', 's', 'x', 'z' -> 3
                    'd', 't' -> 4
                    'l' -> 5
                    'm', 'n' -> 6
                    'r' -> 7
                    else -> 8
                }

                if (x != 8 && x != prev) {
                    ex = ex * 10 + x
                }
                prev = x
            }
            return ex
        }
    }
}
