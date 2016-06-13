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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by mdecorde on 15/05/16.
 */
public class BabyNameProject implements Serializable {

    protected boolean needSaving = false;
    int loop = 0;
    protected String ID;
    protected HashSet<String> genders = new HashSet<>();
    protected HashSet<String> origins = new HashSet<>();
    protected Pattern pattern = null;
    protected HashMap<Integer, Integer> scores = new HashMap<>();
    protected int max = 0;
    protected Integer iMax = null;
    protected Integer currentBabyNameIndex = -1;
    protected List<Integer> nexts = new ArrayList<Integer>();

    public BabyNameProject() {
        genders.add(NameData.M);
        genders.add(NameData.F);
        pattern = Pattern.compile(".*");
        ID = UUID.randomUUID().toString();
    }

    public String getID() {
        return ID;
    }

    public void setNeedToBeSaved(boolean s) {
        needSaving = s;
    }

    public String toString() {
        String l1 = "A ";
        if (genders.contains(NameData.F) && genders.contains(NameData.M)) {
            l1 += "boy or a girl which name";
        } else if (genders.contains(NameData.M)) {
            l1 += "boy which name";
        } else {
            l1 += "girl which name";
        }

        if (origins.size() == 1) {
            l1 += "\n\t origin is '" + origins.toArray()[0] + "'";
        } else if (origins.size() > 1) {
            l1 += "\n\t origins are " + origins;
        } else {
            l1 += "\n\t has no specific origin";
        }

        if (pattern != null) {
            if (".*".equals(pattern.toString())) {
                l1 += "\n\t has no specific pattern";
            } else {
                l1 += "\n\t matches with '" + pattern + "'";
            }
        }

        if (nexts.size() == 1) {
            l1 += "\n\tone remaining names to review";
        } else if (nexts.size() == 0) {
            int n = scores.size();
            if (n > 11) n = n - 10;
            l1 += "\n\tno remaining names to review (loop="+loop+"). Start a new review loop with "+n+" names ?";
        } else {
            l1 += "\n\t"+nexts.size()+" remaining names to review";
        }

        if (scores.size() > 0 && getBest() != null) {
            l1 += "\n\n\tBest match is '" + getBest() + "'";
        }

        return l1;
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
        return MainActivity.database.get(iMax);
    }

    public HashSet<String> getGenders() {
        return genders;
    }

    public HashSet<String> getOrigins() {
        return origins;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public HashSet<String> setGenres(HashSet<String> genres) {
        this.genders = genres;
        return genres;
    }

    public HashSet<String> setOrigins(HashSet<String> origins) {
        this.origins = origins;
        return origins;
    }

    public Pattern setPattern(Pattern pattern) {
        this.pattern = pattern;
        return pattern;
    }

    public boolean isNameValid(BabyName name) {
        //return true;
        //AppLogger.info("test gender " + name+" " + name.genres + " against project genres " + this.getGenders());
        if (this.genders.size() > 0) {
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
        if (this.origins.size() > 0) {
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

            for (int k : scores.keySet()) nexts.add(k); // get all indices

            if (nexts.size() > 11) {
                Collections.sort(nexts, new Comparator<Integer>() {
                    @Override
                    public int compare(Integer i1, Integer i2) {
                        return scores.get(i1) - scores.get(i2);
                    }
                });

                for (int i : nexts.subList(0, 10)) { scores.remove(i); } // remove the scores as well
                nexts = nexts.subList(10, nexts.size()); // remove the 10 worst scores
            }
        } else { // first initialisation

            //AppLogger.info("Build nexts name random list " + MainActivity.database.size());
            for (int i = 1; i < MainActivity.database.size(); i++) {
                if (isNameValid(MainActivity.database.get(i)))
                    nexts.add(i);
            }
        }

        Collections.shuffle(nexts);
        //AppLogger.info("nexts ("+nexts.size()+")= " + nexts);
        loop++;
        return nexts.size() > 0;
    }

    protected BabyName nextName() {
        if (nexts.size() == 0) {
            currentBabyNameIndex = -1;
            return null;
        }

        BabyName currentBabyName;

        int next = nexts.remove(0);
        //AppLogger.info("Next name index: " + next + " from " + MainActivity.database.size() + " choices.");
        currentBabyName = MainActivity.database.get(next);
        if (currentBabyName == null) {
            if (nexts.size() == 0) {
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
        try {
            FileOutputStream fos = context.openFileOutput(project.getID() + ".baby", MainActivity.MODE_WORLD_READABLE);
            ObjectOutputStream serializer = new ObjectOutputStream(fos);
            serializer.writeObject(project);
            fos.close();

            project.setNeedToBeSaved(false);
        } catch (Exception e) {
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
        Collections.sort(names, new Comparator<Integer>() {
            @Override
            public int compare(Integer b1, Integer b2) {
                return BabyNameProject.this.scores.get(b2) - BabyNameProject.this.scores.get(b1);
            }
        });

        //AppLogger.info("names after sort: "+names);
        int min = Math.min(10, names.size());
        return names.subList(0, min);
    }
}

