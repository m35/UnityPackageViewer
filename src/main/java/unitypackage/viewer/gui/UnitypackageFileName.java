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
import javax.swing.filechooser.FileFilter;

public class UnitypackageFileName {

    public static boolean isUnitypackage(File file) {
        return file.isFile() && isUnitypackage(file.getName());
    }

    public static boolean isUnitypackage(String fileName) {
        return fileName.toLowerCase().endsWith(".unitypackage");
    }

    public static class UnitypackageFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            return !f.isFile() || isUnitypackage(f);
        }

        @Override
        public String getDescription() {
            return "*.unitypackage";
        }
    }

}
