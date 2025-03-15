package fr.hnit.babyname

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.util.Collections
import java.util.UUID
import java.util.regex.Pattern
import kotlin.math.min

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

class BabyNameProject : Serializable {
    var needSaving = false
    var loop = 0
    var iD: String
    var genders = HashSet<String>()
    var origins = HashSet<String>()
    var pattern: Pattern?
    var scores = HashMap<Int, Int>()
    private var bestScore = 0
    private var bestScoreIndex: Int? = null
    private var currentBabyNameIndex = 0
    var nexts = mutableListOf<Int>()
    var nextsIndex = 0

    init {
        genders.add(BabyNameDatabase.GENDER_MALE)
        genders.add(BabyNameDatabase.GENDER_FEMALE)
        pattern = Pattern.compile(".*")
        iD = UUID.randomUUID().toString()
    }

    fun setNeedToBeSaved(save: Boolean) {
        needSaving = save
    }

    fun evaluateScore(babyname: BabyName, newScore: Int): Boolean {
        if (newScore > bestScore) {
            // update best match
            bestScore = newScore
            bestScoreIndex = babyname.id
        }

        if (!scores.containsKey(babyname.id)) {
            scores[babyname.id] = 0
        }

        val currentScore = scores[babyname.id]!!
        if (currentScore != newScore) {
            scores[babyname.id] = newScore
            return true
        } else {
            return false
        }
    }

     /**
     * @return best baby name match, may be null
     */
    fun getBest(): BabyName? {
        val index = bestScoreIndex ?: return null
        return MainActivity.database.get(index)
    }

    fun isNameValid(name: BabyName?): Boolean {
        if (name == null) {
            return false
        }

        //Log.d("test gender " + name+" " + name.genres + " against project genres " + this.getGenders());
        if (genders.isNotEmpty()) {
            var genderIsOk = false
            for (genre in name.genres) {
                if (genders.contains(genre)) {
                    genderIsOk = true
                    continue
                }
            }
            if (!genderIsOk) {
                return false
            }
        }

        //Log.d("test origin " + name+" " + name.origins + " against project origins " + this.getOrigins());
        if (origins.isNotEmpty()) {
            var originIsOk = false
            for (origin in name.origins) {
                if (origins.contains(origin)) {
                    originIsOk = true
                    continue
                }
            }
            if (!originIsOk) {
                return false
            }
        }

        //Log.d("test pattern " + name+" " + name.name + " against pattern genres " + this.pattern);
        if (pattern != null) {
            return pattern!!.matcher(name.name).matches()
        }
        return true
    }

    fun nextLoop(): Boolean {
        nexts.clear()

        if (loop >= 1) {
            // add all names that were looked at
            nexts.addAll(scores.keys.toList())

            if (nexts.size > 10) {
                // sort by score, lowest scores first
                nexts.sortWith { i1: Int, i2: Int -> scores[i1]!! - scores[i2]!! }

                // remove worst 10%
                val amountToRemove = (nexts.size / 10).toInt()

                // remove the 10 worst scores
                for (i in nexts.subList(0, amountToRemove)) {
                    scores.remove(i)
                }
                nexts = nexts.subList(amountToRemove, nexts.size)
            }
        } else {
            // first initialisation - add all valid names
            for (i in 0 until MainActivity.database.size()) {
                if (isNameValid(MainActivity.database.get(i))) {
                    nexts.add(i)
                }
            }
        }

        nexts.shuffle()

        loop += 1
        nextsIndex = 0
        currentBabyNameIndex = 0

        setNeedToBeSaved(true)

        return nexts.isNotEmpty()
    }

    fun currentName(): BabyName? {
        Log.d(this, "currentName() currentBabyNameIndex: ${currentBabyNameIndex}")
        if (currentBabyNameIndex >= 0 && currentBabyNameIndex < MainActivity.database.size()) {
            return MainActivity.database.get(currentBabyNameIndex)
        } else {
            return null
        }
    }

    fun previousName(): BabyName? {
        Log.d(this, "previousName() nextsIndex: ${nextsIndex}")
        if (nextsIndex > 0 && nextsIndex <= nexts.size) {
            nextsIndex -= 1
            currentBabyNameIndex = nexts[nextsIndex]
            return MainActivity.database.get(currentBabyNameIndex)
        } else {
            //currentBabyNameIndex = 0
            //nextsIndex = 0;
            return null
        }
    }

    fun nextName(): BabyName? {
        Log.d(this, "nextName() nextsIndex: ${nextsIndex}")
        if (nextsIndex >= -1 && (nextsIndex + 1) < nexts.size) {
            nextsIndex += 1
            currentBabyNameIndex = nexts[nextsIndex]
            return MainActivity.database.get(currentBabyNameIndex)
        } else {
            //currentBabyNameIndex = 0
            //nextsIndex = 0;
            return null
        }
    }

    fun reset() {
        scores.clear()
        loop = 0
        nextLoop()
    }

    fun getTop10(): List<Int> {
        val names = scores.keys.toMutableList()

        //Log.d("names before sort: "+names+" scores: "+scores);
        names.sortWith { b1: Int, b2: Int -> scores[b2]!! - scores[b1]!! }

        //Log.d("names after sort: "+names);
        val min = min(10, names.size)
        return names.subList(0, min)
    }

    companion object {
        fun readProject(filename: String?, context: Context): BabyNameProject? {
            var project: BabyNameProject? = null
            try {
                val fis = context.openFileInput(filename)
                val ois = ObjectInputStream(fis)
                project = ois.readObject() as BabyNameProject
                fis.close()
            } catch (e: Exception) {
                e.printStackTrace()
                context.deleteFile(filename)
            }
            return project
        }

        fun storeProject(project: BabyNameProject, context: Context): Boolean {
            val fileName = project.iD + ".baby"
            try {
                val file = File(context.filesDir, fileName)
                if (file.exists() && file.isFile) {
                    if (!file.delete()) {
                        throw Exception("Failed to delete existing file: $fileName")
                    }
                }
                if (!file.createNewFile()) {
                    throw Exception("Failed to create new file: $fileName")
                }
                val fos = FileOutputStream(file)
                val serializer = ObjectOutputStream(fos)
                serializer.writeObject(project)
                fos.close()

                project.setNeedToBeSaved(false)
            } catch (e: Exception) {
                Log.e(this, "Cannot open $fileName")
                e.printStackTrace()
                return false
            }

            return true
        }
    }
}
