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
import android.util.Log;

/**
 * Created by mdecorde on 16/05/16.
 */
public class AppLogger {
    public static final String LOG = "fr.hnit.babyname";

    public static void error(String message) {
        Log.e(LOG, message);
    }

    public static void warning(String message) {
        Log.w(LOG, message);
    }

    public static void info(String message) {
        Log.i(LOG, message);
    }
}
