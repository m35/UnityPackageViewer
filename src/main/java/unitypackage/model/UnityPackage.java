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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Indexes a .unitypackage and provides methods to read assets out of it.
 */
public class UnityPackage {

    private static final String ROOT_ICON = ".icon.png";

    private final File unitypackageFile;
    private final List<UnityAsset> unityAssetList;

    public UnityPackage(File unitypackageFile) throws IOException {
        this.unitypackageFile = unitypackageFile;

        // TODO are empty asset directories possible?
        // that would break this program

        TreeMap<String, UnityAssetBuilder> rootGuidDirectories = new TreeMap<>();

        try (TarArchiveInputStream tarInput = getTarInputStream()) {

            boolean hasDotRootDirectory = false;

            TarArchiveEntry tarEntry;
            while ((tarEntry = tarInput.getNextEntry()) != null) {

                final String rawFilePathString = tarEntry.getName();
                final boolean isDirectory = tarEntry.isDirectory();

                Path rawPath = Paths.get(rawFilePathString);

                if (rawPath.getNameCount() == 1) {
                    String shouldBeDirName = rawPath.getName(0).toString();
                    if (isDirectory) {
                        if (".".equals(shouldBeDirName)) {
                            hasDotRootDirectory = true;
                            continue;
                        }
                    } else if (!ROOT_ICON.equals(shouldBeDirName)) {
                        // I don't know if the root ".icon.png" would be next to a root "." or under it
                        throw new RuntimeException("Found root path that is not a directory or " + ROOT_ICON + ": " + rawFilePathString);
                    }

                }

                if (hasDotRootDirectory) {
                    // Trim off the "." before continuing
                    rawPath = rawPath.subpath(1, rawPath.getNameCount());
                }

                Path rawPathParent = rawPath.getParent();

                String guidDirectory;
                String fileName;

                if (isDirectory) {
                    if (rawPathParent != null) {
                        throw new RuntimeException("Found nested directory \"" + rawFilePathString + "\"");
                    }
                    guidDirectory = rawPath.toString();
                    fileName = null;
                } else {
                    fileName = rawPath.getFileName().toString();
                    guidDirectory = rawPathParent == null ? null : rawPathParent.toString();
                }

                if (guidDirectory == null) {
                    if (fileName.equals(ROOT_ICON)) {
                        // Image icon exists in the root
                        // TODO do something with this
                        // For now ignore it
                        continue;
                    } else {
                        throw new RuntimeException("Found nested directory \"" + rawFilePathString + "\"");
                    }
                }

                UnityAssetBuilder builder = rootGuidDirectories.get(guidDirectory);

                if (builder == null) {
                    if (isDirectory) {
                        builder = new UnityAssetBuilder(guidDirectory);
                    } else {
                        // Do .tar archives always put a directory definition before any files under it?
                        // In any case, be flexible.
                        builder = new UnityAssetBuilder(guidDirectory, fileName, tarEntry, tarInput);
                    }
                    rootGuidDirectories.put(guidDirectory, builder);
                } else {
                    if (isDirectory)
                        builder.assertGuidMatchesDirectoryName(guidDirectory);
                    else
                        builder.addFileFoundInDirectory(guidDirectory, fileName, tarEntry, tarInput);
                }
            }
        }

        List<UnityAsset> assets = rootGuidDirectories
                .values()
                .stream()
                .map(tad -> tad.makeUnityAsset())
                .collect(Collectors.toList());

        unityAssetList = Collections.unmodifiableList(assets);
    }

    public File getUnitypackageFile() {
        return unitypackageFile;
    }

    public List<UnityAsset> getUnityAssetList() {
        return unityAssetList;
    }

    final public TarArchiveInputStream getTarInputStream() throws IOException {
        return new TarArchiveInputStream(new GZIPInputStream(new FileInputStream(unitypackageFile)));
    }

    public UnityArchiveInputStream getUnityArchiveInputStream() throws IOException {
        return new UnityArchiveInputStream(this);
    }

    public InputStream getFileStream(UnityAsset assetToExtract) throws IOException {

        UnityArchiveInputStream unityInputStream = new UnityArchiveInputStream(getTarInputStream(),
                                                                               Collections.singletonList(assetToExtract));

        UnityAsset assetFound = unityInputStream.getNextEntry();

        // Sanity check
        if (assetToExtract != assetFound) {
            throw new IllegalArgumentException("This should never happen");
        }

        return unityInputStream;
    }

}
