package fr.hnit.babyname

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
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

class BabyNameProject() : Serializable {
    var needSaving = false
    var iD: String = UUID.randomUUID().toString()
    var genders = HashSet<String>()
    var origins = HashSet<String>()
    var pattern: Pattern? = null
    var scores = HashMap<Int, Int>()
    var nexts = mutableListOf<Int>()
    var nextsIndex = 0

    init {
        genders.add(BabyNameDatabase.GENDER_MALE)
        genders.add(BabyNameDatabase.GENDER_FEMALE)
        pattern = Pattern.compile(".*")
        reset()
    }

    fun cloneProject(): BabyNameProject {
        val project = BabyNameProject()
        project.genders = genders.toHashSet()
        project.origins = origins.toHashSet()
        project.pattern = pattern
        project.scores = HashMap(scores)
        project.nexts = nexts.toMutableList()
        project.nextsIndex = nextsIndex
        return project
    }

    fun setNeedToBeSaved(save: Boolean) {
        needSaving = save
    }

    fun getBest(): BabyName? {
        var bestScoreIndex = -1
        var bestScoreValue = 0

        for (entry in scores.iterator()) {
            if (entry.value > bestScoreValue) {
                bestScoreIndex = entry.key
                bestScoreValue = entry.value
            }
        }

        if (bestScoreIndex == -1) {
            return null
        } else {
            return MainActivity.database.get(bestScoreIndex)
        }
    }

    fun isNameValid(name: BabyName?): Boolean {
        if (name == null) {
            return false
        }

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

        if (pattern != null) {
            return pattern!!.matcher(name.name).matches()
        }
        return true
    }

    fun dropLast() {
        if (nexts.size > 10) {
            val amountToRemove = ((BabyNameProject.DROP_RATE_PERCENT *  nexts.size) / 100)

            val removed = nexts.subList(nexts.size - amountToRemove, nexts.size)

            // update scores
            for (i in removed) {
                scores.remove(i)
            }

            nexts = nexts.subList(0, nexts.size - amountToRemove)
            setNeedToBeSaved(true)
        }
    }

    fun removeCurrent() {
        if (nextsIndex >= 0 && nextsIndex < nexts.size) {
            scores.remove(nexts[nextsIndex])
            nexts.removeAt(nextsIndex)
        }
    }

    fun nextRound() {
        // sort by score, lowest scores last
        nexts.sortWith { i1: Int, i2: Int -> (scores[i2] ?: 0) - (scores[i1] ?: 0 ) }

        dropLast()

        nexts.shuffle()

        nextsIndex = 0

        setNeedToBeSaved(true)
    }

    fun currentName(): BabyName? {
        if (nextsIndex >= 0 && nextsIndex < nexts.size) {
            return MainActivity.database.get(nexts[nextsIndex])
        } else {
            return null
        }
    }

    fun previousName(): BabyName? {
        if (nextsIndex > 0 && nextsIndex <= nexts.size) {
            nextsIndex -= 1
            return MainActivity.database.get(nexts[nextsIndex])
        } else {
            return null
        }
    }

    fun nextName(): BabyName? {
        if (nextsIndex >= -1 && (nextsIndex + 1) < nexts.size) {
            nextsIndex += 1
            return MainActivity.database.get(nexts[nextsIndex])
        } else {
            return null
        }
    }

    fun rebuildNexts() {
        nexts.clear()
        for (i in 0 until MainActivity.database.size()) {
            if (isNameValid(MainActivity.database.get(i))) {
                nexts.add(i)
            }
        }
    }

    fun reset() {
        scores.clear()

        rebuildNexts()

        nexts.shuffle()

        nextsIndex = 0

        setNeedToBeSaved(true)
    }

    fun getTop10(): List<Int> {
        val names = scores.keys.toMutableList()

        //Log.d("names before sort: "+names+" scores: "+scores);
        names.sortWith { b1: Int, b2: Int -> (scores[b2] ?: 0) - (scores[b1] ?: 0) }

        //Log.d("names after sort: "+names);
        val min = min(10, names.size)
        return names.subList(0, min)
    }

    companion object {
        const val DROP_RATE_PERCENT = 20

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
