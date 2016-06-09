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
import android.util.SparseArray;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by mdecorde on 16/05/16.
 */
public class BabyNameDatabase extends SparseArray<BabyName> {

    private static int BABYCOUNTER = 0;

    public void initialize() {
        for (int i = 0 ; i < NameData.data.length ; i = i+3) {
            HashSet<String> genres = new HashSet<>(Arrays.asList(NameData.data[i+1].split(",")));
            HashSet<String> origins = new HashSet<>(Arrays.asList(NameData.data[i+2].split(",")));
            BabyName b = new BabyName(NameData.data[i], genres, origins);
            this.put(b.id, b);
        }

        for (int i = 0 ; i < NameData2.data.length ; i = i+3) {
            HashSet<String> genres = new HashSet<>(Arrays.asList(NameData2.data[i+1].split(",")));
            HashSet<String> origins = new HashSet<>(Arrays.asList(NameData2.data[i+2].split(",")));
            BabyName b = new BabyName(NameData2.data[i], genres, origins);
            this.put(b.id, b);
        }

        for (int i = 0 ; i < NameData3.data.length ; i = i+3) {
            HashSet<String> genres = new HashSet<>(Arrays.asList(NameData3.data[i+1].split(",")));
            HashSet<String> origins = new HashSet<>(Arrays.asList(NameData3.data[i+2].split(",")));
            BabyName b = new BabyName(NameData3.data[i], genres, origins);
            this.put(b.id, b);
        }

        for (int i = 0 ; i < NameData4.data.length ; i = i+3) {
            HashSet<String> genres = new HashSet<>(Arrays.asList(NameData4.data[i+1].split(",")));
            HashSet<String> origins = new HashSet<>(Arrays.asList(NameData4.data[i+2].split(",")));
            BabyName b = new BabyName(NameData4.data[i], genres, origins);
            this.put(b.id, b);
        }
        for (int i = 0 ; i < NameData5.data.length ; i = i+3) {
            HashSet<String> genres = new HashSet<>(Arrays.asList(NameData5.data[i+1].split(",")));
            HashSet<String> origins = new HashSet<>(Arrays.asList(NameData5.data[i+2].split(",")));
            BabyName b = new BabyName(NameData5.data[i], genres, origins);
            this.put(b.id, b);
        }
    }

    public static int getNextBabyID() {
        BABYCOUNTER++;
        return BABYCOUNTER;
    }
}
