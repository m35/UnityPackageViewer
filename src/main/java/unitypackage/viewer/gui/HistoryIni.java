/*
 * Basic .unitypackage Viewer
 * Copyright (C) 2024 Michael Sabin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package unitypackage.viewer.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * Just stores the last .unitypackage file that was opened.
 * Additional settings could be added here if needed.
 */
public class HistoryIni {

    private static final String INI_FILE = "history.ini";

    public static void addLastFile(File file) {
        try (PrintStream p = new PrintStream(new FileOutputStream(INI_FILE))) {
            p.println(file.toString());
        } catch (FileNotFoundException ex) {
            System.out.println("[ERROR] Unable to save " + INI_FILE);
            ex.printStackTrace(System.out);
        }
    }

    public static File getLastDirectory() {
        List<String> lastFiles = getLastFiles();
        if (!lastFiles.isEmpty()) {
            String lf = lastFiles.get(lastFiles.size()-1);
            File lff = new File(lf).getParentFile();

            return lff;
        }
        return null;
    }

    private static List<String> getLastFiles() {
        try {
            return Files.readAllLines(Paths.get(INI_FILE), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
            return Collections.emptyList();
        }
    }

}
