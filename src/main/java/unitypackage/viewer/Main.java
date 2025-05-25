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

package unitypackage.viewer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import unitypackage.model.UnityArchiveInputStream;
import unitypackage.model.UnityAsset;
import unitypackage.model.UnityPackage;
import unitypackage.viewer.gui.MainWindow;

public class Main {

    private static final String VERSION_PROPERTY_FILE = "app.properties";
    private static final String DEVELOPMENT_VERSION = "(development)";
    public static String VERSION = DEVELOPMENT_VERSION;

    private static final String EXTRACT_ALL_COMMAND = "--extract-all";

    /**
     * Looks for the file {@link #VERSION_PROPERTY_FILE} that should have been filtered
     * into the build with the application's version. But if not found, will default
     * to {@link #DEVELOPMENT_VERSION}.
     */
    private static void initVersion() {

        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream propertyStream = classLoader.getResourceAsStream(VERSION_PROPERTY_FILE)) {
            if (propertyStream != null) {
                Properties properties = new Properties();
                properties.load(propertyStream);
                String version = properties.getProperty("version");
                if (version != null) {
                    VERSION = "(v"+version + ")";
                }
            }
        } catch (IOException ex) {
            System.out.println("Error loading version " + ex.getMessage());
        }
    }

    public static void main(String[] args) throws IOException {

        initVersion();

        ArrayList<String> argsList = new ArrayList<>(Arrays.asList(args));

        boolean hasExtractAllCommand = false;
        String fileToOpen = null;

        if (!argsList.isEmpty()) {
            hasExtractAllCommand = argsList.remove(EXTRACT_ALL_COMMAND);
            if (!argsList.isEmpty()) {
                fileToOpen = argsList.get(0);
            }
        }

        if (hasExtractAllCommand) {
            if (fileToOpen == null) {
                System.out.println(EXTRACT_ALL_COMMAND + " expects a file to extract");
                System.exit(1);
            }
            extractAll(fileToOpen);
        } else {
            runGui(fileToOpen);
        }
    }

    private static void extractAll(String fileToOpen) throws IOException {
        File file = new File(fileToOpen);

        UnityPackage unityPackage = new UnityPackage(file);
        try (UnityArchiveInputStream unityIS = unityPackage.getUnityArchiveInputStream()) {
            UnityAsset nextAsset;
            while ((nextAsset = unityIS.getNextEntry()) != null) {
                Path assetPath = nextAsset.getFullPathAsPath();
                Path parentDirectory = assetPath.getParent();
                if (parentDirectory != null) {
                    Files.createDirectories(parentDirectory);
                }

                Files.copy(unityIS, assetPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Extracted " + assetPath);
            }
        }
    }

    private static void runGui(String fileToOpen) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow frame = new MainWindow(fileToOpen);
                frame.setVisible(true);
            }
        });

    }

}
