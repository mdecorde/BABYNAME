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
    var needSaving: Boolean = false
    var loop: Int = 0
    var iD: String
    var genders = HashSet<String>()
    var origins = HashSet<String>()
    var pattern: Pattern?
    var scores = HashMap<Int, Int>()
    var max: Int = 0
    private var iMax: Int? = null
    var currentBabyNameIndex: Int = -1
    var nexts = mutableListOf<Int>()

    init {
        genders.add(BabyNameDatabase.GENDER_MALE)
        genders.add(BabyNameDatabase.GENDER_FEMALE)
        pattern = Pattern.compile(".*")
        iD = UUID.randomUUID().toString()
    }

    fun setNeedToBeSaved(s: Boolean) {
        needSaving = s
    }

    fun evaluate(babyname: BabyName, score: Int): Int {
        var score = score
        if (!scores.containsKey(babyname.id)) {
            scores[babyname.id] = 0
        }
        score += scores[babyname.id]!!
        if (score > max) { // update best match
            max = score
            iMax = babyname.id
        }
        scores[babyname.id] = score
        return score
    }

     /**
     * @return best baby name match, may be null
     */
    fun getBest(): BabyName? {
        val index = iMax ?: return null
         return MainActivity.database.get(index)
    }

    fun isNameValid(name: BabyName): Boolean {
        //Log.d("test gender " + name+" " + name.genres + " against project genres " + this.getGenders());
        if (genders.isNotEmpty()) {
            var genderIsOk = false
            for (genre in name.genres) {
                if (genders.contains(genre)) {
                    genderIsOk = true
                    continue
                }
            }
            if (!genderIsOk) return false
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
            if (!originIsOk) return false
        }

        //Log.d("test pattern " + name+" " + name.name + " against pattern genres " + this.pattern);
        if (pattern != null) {
            return pattern!!.matcher(name.name).matches()
        }
        return true
    }

    fun rebuildNexts(): Boolean {
        nexts.clear()

        if (loop >= 1) { // uses score to get next names and remove worst scores
            // get all indices
            nexts.addAll(scores.keys.toList())

            if (nexts.size > 11) {
                nexts.sortWith { i1: Int, i2: Int -> scores[i1]!! - scores[i2]!! }

                val amountToRemove =
                    min(10.0, (nexts.size - 10).toDouble()).toInt()

                for (i in nexts.subList(0, amountToRemove)) {
                    scores.remove(i) // remove the scores as well
                }
                nexts = nexts.subList(amountToRemove, nexts.size) // remove the 10 worst scores
            }
        } else { // first initialisation
            //Log.d("Build nexts name random list " + MainActivity.database.size());
            for (i in 1 until MainActivity.database.size()) {
                if (isNameValid(MainActivity.database.get(i)!!)) {
                    nexts.add(i)
                }
            }
        }

        nexts.shuffle()
        //Log.d("nexts ("+nexts.size()+")= " + nexts);
        loop++
        return nexts.isNotEmpty()
    }

    fun nextName(): BabyName? {
        if (nexts.isEmpty()) {
            currentBabyNameIndex = -1
            return null
        }

        val next = nexts.removeAt(0)
        //Log.d("Next name index: " + next + " from " + MainActivity.database.size() + " choices.");
        val currentBabyName = MainActivity.database.get(next)

        //Log.d("Next: " + currentBabyName);
        //Log.d("Next name: " + currentBabyName.name);
        currentBabyNameIndex = next
        return currentBabyName
    }

    fun reset() {
        nexts.clear()
        scores.clear()
        this.rebuildNexts()
        currentBabyNameIndex = -1
        loop = 0
    }

    fun getTop10(): List<Int> {
        val names = scores.keys.toMutableList()

        //Log.d("names before sort: "+names+" scores: "+scores);
        names.sortWith { b1: Int, b2: Int -> scores[b2]!! - scores[b1]!! }

        //Log.d("names after sort: "+names);
        val min = min(10.0, names.size.toDouble()).toInt()
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
                AppLogger.e(this, "Cannot open $fileName")
                e.printStackTrace()
                return false
            }

            return true
        }
    }
}
