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
import android.util.SparseArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by mdecorde on 16/05/16.
 */
public class BabyNameDatabase extends SparseArray<BabyName>
{
    public static final String GENDER_MALE = "m";
    public static final String GENDER_FEMALE = "f";
    //public static final String GENDER_NEUTRAL = "n";

    public void initialize(Context ctx) {
        Thread thread = new Thread() {
            public void run() {
                try {
                    String databaseFileName = "babynames.csv";
                    BufferedReader reader = new BufferedReader(new InputStreamReader(ctx.getAssets().open(databaseFileName)));
                    int lineNumber = 0;
                    while (reader.ready()) {
                        lineNumber += 1;
                        String line = reader.readLine();
                        String[] items = line.split(";", -1);
                        if (items.length != 3) {
                            AppLogger.error("Failed to parse line in " + databaseFileName + ":"  + lineNumber +  ": " + line);
                            break;
                        }

                        String name = items[0];
                        HashSet<String> genres = new HashSet<>(Arrays.asList(items[1].split(",", -1)));
                        HashSet<String> origins = new HashSet<>(Arrays.asList(items[2].split(",", -1)));

                        // remove empty entries
                        genres.remove("");
                        origins.remove("");

                        if (!name.isEmpty()) {
                            BabyName b = new BabyName(name, genres, origins);
                            BabyNameDatabase.this.put(b.id, b);
                        } else {
                            AppLogger.error("Empty baby name in " + databaseFileName + ":"  + lineNumber +  ": " + line);
                        }
                    }
                    reader.close();

                    AppLogger.info("Loaded " +  BabyNameDatabase.this.size() + " names");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    HashSet<String> getAllOrigins() {
        HashSet<String> all = new HashSet<>();
        int n = this.size();
        for (int i = 0; i < n; i++) {
            BabyName entry = this.get(i);
            if (entry != null) {
                all.addAll(entry.origins);
            }
        }
        return all;
    }
}
