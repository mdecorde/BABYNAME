package fr.hnit.babyname;
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

import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by mdecorde on 15/05/16.
 */
public class BabyNameProject implements Serializable
{
    protected boolean needSaving = false;
    protected int loop = 0;
    protected String id;
    protected HashSet<String> genders = new HashSet<>();
    protected HashSet<String> origins = new HashSet<>();
    protected Pattern pattern;
    protected HashMap<Integer, Integer> scores = new HashMap<>();
    protected int max = 0;
    protected Integer iMax = null;
    protected Integer currentBabyNameIndex = -1;
    protected List<Integer> nexts = new ArrayList<Integer>();

    public BabyNameProject() {
        genders.add(BabyNameDatabase.GENDER_MALE);
        genders.add(BabyNameDatabase.GENDER_FEMALE);
        pattern = Pattern.compile(".*");
        id = UUID.randomUUID().toString();
    }

    public String getID() {
        return id;
    }

    public void setNeedToBeSaved(boolean s) {
        needSaving = s;
    }

    public int evaluate(BabyName babyname, int score) {
        if (!scores.containsKey(babyname.id)) {
            scores.put(babyname.id, 0);
        }
        score += scores.get(babyname.id);
        if (score > max) { // update best match
            max = score;
            iMax = babyname.id;
        }
        scores.put(babyname.id, score);
        return score;
    }

    /**
     * @return best baby name match, may be null
     */
    public BabyName getBest() {
        if (iMax == null) {
            return null;
        }
        return MainActivity.database.get(iMax);
    }

    public HashSet<String> getGenders() {
        return genders;
    }

    public HashSet<String> getOrigins() {
        return origins;
    }

    public void setGenres(HashSet<String> genres) {
        this.genders = genres;
    }

    public void setOrigins(HashSet<String> origins) {
        this.origins = origins;
    }

    public void setPattern(Pattern pattern) {
        this.pattern = pattern;
    }

    public boolean isNameValid(BabyName name) {
        //AppLogger.info("test gender " + name+" " + name.genres + " against project genres " + this.getGenders());
        if (!this.genders.isEmpty()) {
            boolean genderIsOk = false;
            for (String genre : name.genres) {
                if (this.genders.contains(genre)) {
                    genderIsOk = true;
                    continue;
                }
            }
            if (!genderIsOk) return false;
        }

        //AppLogger.info("test origin " + name+" " + name.origins + " against project origins " + this.getOrigins());
        if (!this.origins.isEmpty()) {
            boolean originIsOk = false;
            for (String origin : name.origins) {
                if (this.origins.contains(origin)) {
                    originIsOk = true;
                    continue;
                }
            }
            if (!originIsOk) return false;
        }

        //AppLogger.info("test pattern " + name+" " + name.name + " against pattern genres " + this.pattern);
        if (pattern != null) {
            return pattern.matcher(name.name).matches();
        }
        return true;
    }

    protected boolean rebuildNexts() {
        nexts.clear();

        if (loop >= 1) { // uses score to get next names and remove worst scores
            // get all indices
            nexts.addAll(scores.keySet());

            if (nexts.size() > 11) {
                Collections.sort(nexts, (i1, i2) -> scores.get(i1) - scores.get(i2));

                int amountToRemove = Math.min(10, nexts.size() - 10);

                for (int i : nexts.subList(0, amountToRemove)) {
                    scores.remove(i); // remove the scores as well
                }
                nexts = nexts.subList(amountToRemove, nexts.size()); // remove the 10 worst scores
            }
        } else { // first initialisation
            //AppLogger.info("Build nexts name random list " + MainActivity.database.size());
            for (int i = 1; i < MainActivity.database.size(); i++) {
                if (isNameValid(MainActivity.database.get(i))) {
                    nexts.add(i);
                }
            }
        }

        Collections.shuffle(nexts);
        //AppLogger.info("nexts ("+nexts.size()+")= " + nexts);
        loop++;
        return !nexts.isEmpty();
    }

    protected BabyName nextName() {
        if (nexts.isEmpty()) {
            currentBabyNameIndex = -1;
            return null;
        }

        int next = nexts.remove(0);
        //AppLogger.info("Next name index: " + next + " from " + MainActivity.database.size() + " choices.");
        BabyName currentBabyName = MainActivity.database.get(next);
        if (currentBabyName == null) {
            if (nexts.isEmpty()) {
                currentBabyNameIndex = -1;
                return null;
            }
        }
        //AppLogger.info("Next: " + currentBabyName);
        //AppLogger.info("Next name: " + currentBabyName.name);

        currentBabyNameIndex = next;
        return currentBabyName;
    }

    public static BabyNameProject readProject(String filename, Context context) {
        BabyNameProject project = null;
        try {
            FileInputStream fis = context.openFileInput(filename);
            ObjectInputStream ois = new ObjectInputStream(fis);
            project = (BabyNameProject) ois.readObject();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            context.deleteFile(filename);
        }
        return project;
    }

    public static boolean storeProject(BabyNameProject project, Context context) {
        String fileName = project.getID() + ".baby";
        try {
            File file = new File(context.getFilesDir(), fileName);
            if (file.exists() && file.isFile()) {
                if (!file.delete()) {
                    throw new Exception("Failed to delete existing file: " + fileName);
                }
            }
            if (!file.createNewFile()) {
                throw new Exception("Failed to create new file: " + fileName);
            }
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream serializer = new ObjectOutputStream(fos);
            serializer.writeObject(project);
            fos.close();

            project.setNeedToBeSaved(false);
        } catch (Exception e) {
            AppLogger.error("Cannot open " + fileName);
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void reset() {
        this.nexts.clear();
        this.scores.clear();
        this.rebuildNexts();
        currentBabyNameIndex = -1;
        loop = 0;
    }

    public List<Integer> getTop10() {
        List<Integer> names = new ArrayList<Integer>(this.scores.size());
        names.addAll(this.scores.keySet());

        //AppLogger.info("names before sort: "+names+" scores: "+scores);
        Collections.sort(names, (b1, b2) -> BabyNameProject.this.scores.get(b2) - BabyNameProject.this.scores.get(b1));

        //AppLogger.info("names after sort: "+names);
        int min = Math.min(10, names.size());
        return names.subList(0, min);
    }
}
