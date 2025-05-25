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

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * An asset as it would appear in the tree of stuff you see when you import a .unitypackage into Unity.
 * This could be a directory or a file.
 */
public class UnityAsset {

    private final UnityAssetBuilder source;

    UnityAsset(UnityAssetBuilder source) {
        this.source = source;
    }

    public String getFullPath() {
        return source.getPathname_firstLine();
    }

    public Path getFullPathAsPath() {
        return Paths.get(getFullPath());
    }

    public Path getFileNameAsPath() {
        return getFullPathAsPath().getFileName();
    }

    public String getFileName() {
        return getFileNameAsPath().toString();
    }

    /**
     * Returns -1 if the asset is not a file.
     */
    public long getSize() {
        return source.getAsset_fileSize();
    }

    public String getGuid() {
        return source.getAsset_meta_guid();
    }

    public String getDirectoryGuid() {
        return source.getGuidBaseDirectory();
    }

    /**
     * May be null.
     */
    public BufferedImage getPreview() {
        return source.getPreview();
    }

    /**
     * In the .unitypackage, the originating directory does not contain a file named "asset",
     * which probably means it represents a directory (in practice this seems to be the case).
     */
    public boolean isProbablyDirectory() {
        return source.getRawPathTo_asset_file() == null;
    }

    String getTarPathOf_asset_File() {
        return source.getRawPathTo_asset_file();
    }

    public Date getDateModified() {
        return source.getAsset_dateModified();
    }

    @Override
    public String toString() {
        return "UnityAsset{" + getFullPath() + '}';
    }
}
