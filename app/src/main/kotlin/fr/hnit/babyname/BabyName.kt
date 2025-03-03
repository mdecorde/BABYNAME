package fr.hnit.babyname

import java.io.Serializable

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

class BabyName(var name: String, genres: HashSet<String>, origins: HashSet<String>) : Serializable {
    var id: Int
    var genres: HashSet<String> = HashSet()
    var origins: HashSet<String> = HashSet()

    init {
        this.genres = genres
        this.origins = origins
        this.id = nextId++
    }

    companion object {
        private var nextId = 0
    }
}
