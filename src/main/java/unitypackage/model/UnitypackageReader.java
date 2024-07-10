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

package unitypackage.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * .unitypackage files are actually pretty simple.
 *
 * It's a .tar file with a flat list of directories of the GUIDs in the package.
 * Inside each directory will contain files with these names:
 *
 * "pathname" = Contains one line of text that is the full path of the asset where it will appear when imported into Unity.
 * "asset.meta" = The corresponding .meta file.
 * "asset" = Contains the actual asset payload. Won't exist for directories.
 * "preview.png" = Optional preview of some types of assets.
 */
public class UnitypackageReader {

    /**
     * Gets the stream of the file being requested from the given unitypackage.
     * @param unitypackageFile Should be the original file passed to {@link #readAssetsList(File)}.
     *                         If asset is not found will throw {@link IllegalArgumentException}.
     */
    public static InputStream getFileStream(UnityAsset assetToExtract, File unitypackageFile) throws IOException {

        TarArchiveInputStream tarInput = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(unitypackageFile)));

        String tarPathOf_asset_file = assetToExtract.getTarPathOf_asset_File();

        TarArchiveEntry entry;
        // Seek for the file of interest
        while ((entry = tarInput.getNextEntry()) != null) {
            if (entry.getName().equals(tarPathOf_asset_file))  {
                return tarInput;
            }
        }

        tarInput.close();

        throw new IllegalArgumentException("Could not find asset " + assetToExtract.getFullPath());
    }

    public static List<UnityAsset> readAssetsList(File unitypackageFile) throws IOException {

        // TODO are empty asset directories possible?
        // that would break this program

        TreeMap<String, UnityAssetBuilder> rootGuidDirectories = new TreeMap<>();

        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(unitypackageFile)))) {
            TarArchiveEntry tarEntry;

            while ((tarEntry = tarInput.getNextEntry()) != null) {

                String rawFilePath = tarEntry.getName();
                File rawFile = new File(rawFilePath);

                final boolean isDirectory = tarEntry.isDirectory();

                String guidBaseDirectory;
                if (isDirectory) {
                    if (rawFile.getParent() != null) {
                        // afaik .unitypackage should only have 1 level of directories
                        throw new RuntimeException("Found nested directory " + rawFilePath);
                    }
                    guidBaseDirectory = rawFile.getPath();
                } else {
                    guidBaseDirectory = rawFile.getParent();
                }

                if (guidBaseDirectory == null) {
                    if (".icon.png".equals(rawFilePath)) {
                        // Image icon exists in the root
                        // TODO do something with this
                        // For now ignore it
                    } else {
                        throw new RuntimeException("Found nested directory " + rawFilePath);
                    }

                } else {
                    UnityAssetBuilder builder = rootGuidDirectories.get(guidBaseDirectory);

                    if (builder == null) {
                        if (isDirectory) {
                            builder = new UnityAssetBuilder(guidBaseDirectory);
                        } else {
                            // Do .tar archives always put a directory definition before any files under it?
                            // In any case, be flexible.
                            builder = new UnityAssetBuilder(tarEntry, tarInput);
                        }
                        rootGuidDirectories.put(guidBaseDirectory, builder);
                    } else {
                        if (isDirectory)
                            builder.assertGuidMatchesDirectoryName(guidBaseDirectory);
                        else
                            builder.addFileFoundInDirectory(tarEntry, tarInput);
                    }
                }
            }
        }

        List<UnityAsset> assets = rootGuidDirectories
                .values()
                .stream()
                .map(tad -> tad.makeUnityAsset())
                .collect(Collectors.toList());

        return assets;
    }


}
