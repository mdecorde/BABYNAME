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
import java.io.Serializable;
import java.util.HashSet;

/**
 * Created by mdecorde on 16/05/16.
 */
public class BabyName implements Serializable {

    protected static Integer nextId = 0;
    protected Integer id;
    protected String name;
    protected HashSet<String> genres = new HashSet<>();
    protected HashSet<String> origins = new HashSet<>();

    public BabyName(String name, HashSet<String> genres, HashSet<String> origins) {
        this.name = name;
        this.genres = genres;
        this.origins = origins;
        this.id = nextId++;
    }

    public BabyName(String name, String genre, String origin) {
        this.name = name;
        this.genres.add(genre);
        this.origins.add(origin);
        this.id = nextId++;
    }

    public String toString() {
        return ""+name;
    }
}
