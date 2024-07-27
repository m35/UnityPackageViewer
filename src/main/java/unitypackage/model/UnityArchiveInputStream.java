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

import java.io.FilterInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Like {@link TarArchiveInputStream} but for .unitypackage files,
 * except it skips all directories.
 */
public class UnityArchiveInputStream extends FilterInputStream {

    private final TarArchiveInputStream tarInputStream;
    private final Map<String, UnityAsset> tarPathsToUnityAsset = new TreeMap<>();

    public UnityArchiveInputStream(UnityPackage unityPackage) throws IOException {
        this(unityPackage.getTarInputStream(), unityPackage.getUnityAssetList());
    }

    public UnityArchiveInputStream(TarArchiveInputStream tarInputStream, List<UnityAsset> unityAssetsToRead) {
        super(tarInputStream);
        this.tarInputStream = tarInputStream;

        for (UnityAsset unityAsset : unityAssetsToRead) {
            if (!unityAsset.isProbablyDirectory()) {
                tarPathsToUnityAsset.put(unityAsset.getTarPathOf_asset_File(), unityAsset);
            }
        }
    }

    /**
     * Like {@link TarArchiveInputStream#getNextEntry()} but for Unity asset files,
     * except it only returns actual files, skipping directories.
     */
    public UnityAsset getNextEntry() throws IOException {

        TarArchiveEntry entry;
        // Seek for the file of interest
        while ((entry = tarInputStream.getNextEntry()) != null) {
            String tarEntryName = entry.getName();
            // Check if this file in the tar is an asset payload
            UnityAsset unityAsset = tarPathsToUnityAsset.get(tarEntryName);
            if (unityAsset != null) {
                return unityAsset;
            }
        }

        return null;
    }
}
