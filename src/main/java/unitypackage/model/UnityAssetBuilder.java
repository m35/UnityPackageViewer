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
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

/**
 * Collects information about the files inside one directory in the .unitypackage tar file.
 */
class UnityAssetBuilder {

    private static final boolean STRICT = false;

    /**
     * The directory name, which is also the GUID of the asset.
     */
    private final String guidBaseDirectory;

    public String getGuidBaseDirectory() {
        return guidBaseDirectory;
    }

    /**
     * GUID found in the "asset.meta" file in this directory.
     * May be null.
     */
    private String asset_meta_guid;

    public String getAsset_meta_guid() {
        return asset_meta_guid;
    }

    /**
     * Contents of the "pathname" file in this directory.
     */
    private String pathname_firstLine;

    public String getPathname_firstLine() {
        return pathname_firstLine;
    }

    /**
     * Full path in the tar file to the "asset" file in this directory.
     */
    private String rawPathTo_asset_file;

    public String getRawPathTo_asset_file() {
        return rawPathTo_asset_file;
    }

    /**
     * Size of the "asset" file in this directory.
     * Will remain -1 if there is no "asset" file.
     */
    private long asset_fileSize = -1;

    public long getAsset_fileSize() {
        return asset_fileSize;
    }

    private Date asset_dateModified;

    public Date getAsset_dateModified() {
        return asset_dateModified;
    }

    /**
     * The image in the "preview.png" file in this directory.
     * May be null.
     */
    private BufferedImage _preview;

    public BufferedImage getPreview() {
        return _preview;
    }

    public UnityAssetBuilder(String guidBaseDirectory) {
        this.guidBaseDirectory = guidBaseDirectory;
    }

    public UnityAssetBuilder(TarArchiveEntry tarEntry,
                             TarArchiveInputStream tarInputStream) throws IOException
    {
        String rawFilePath = tarEntry.getName();
        File rawFile = new File(rawFilePath);
        guidBaseDirectory = rawFile.getParent();

        addFileFoundInDirectory(tarEntry, tarInputStream);
    }

    public UnityAsset makeUnityAsset() {
        return new UnityAsset(this);
    }

    /**
     * Sanity check that only files that belong in this directory are being added.
     */
    public void assertGuidMatchesDirectoryName(String guid) {
        if (!guid.equals(guidBaseDirectory)) {
            throw new IllegalArgumentException("Argument guid " + guid + " != this guid" +
                                                       " dir " + guidBaseDirectory);
        }
    }

    public void addFileFoundInDirectory(TarArchiveEntry tarEntry,
                                        TarArchiveInputStream tarInputStream) throws IOException {
        String rawFilePath = tarEntry.getName();
        File rawFile = new File(rawFilePath);

        String directoryGuidName = rawFile.getParent();

        assertGuidMatchesDirectoryName(directoryGuidName);

        String rawFileName = rawFile.getName();

        switch (rawFileName) {
            case "asset":
                asset_fileSize = tarEntry.getRealSize();
                rawPathTo_asset_file = rawFilePath;
                asset_dateModified = tarEntry.getLastModifiedDate();
                break;
            case "asset.meta":
                asset_meta_guid = findGuidIn_asset_meta_File(tarEntry, tarInputStream);
                if (!asset_meta_guid.equals(guidBaseDirectory)) {
                    // afaik the directory guid should match the guid in the asset.meta file
                    String s = "Corrupted .unitypackage? directory guid" + " " + guidBaseDirectory + " != " + "asset.meta guid " + asset_meta_guid;
                    if (STRICT) {
                        throw new RuntimeException(s);
                    } else {
                        System.out.println(s);
                    }
                }
                break;
            case "pathname":
                pathname_firstLine = readFirstLine(tarEntry, tarInputStream);
                break;
            case "preview.png":
                _preview = ImageIO.read(tarInputStream);
                break;
            default:
                throw new RuntimeException("File name not recognized " + rawFilePath);
        }
    }

    /**
     * Find the string "guid: " in an "asset.meta" file.
     * @param asset_meta_Stream Stream of the "asset.meta" file contents.
     */
    private static String findGuidIn_asset_meta_File(TarArchiveEntry tarEntry, InputStream asset_meta_Stream) {
        // Would be nice to parse the YAML Unity uses, but it's non-standard
        // so normal parsers will blow up. So will just use manual text parsing.

        final String GUID_LINE_PREFIX = "guid: ";
        List<String> lines = readLines(asset_meta_Stream);
        for (String line : lines) {
            if (line.startsWith(GUID_LINE_PREFIX)) {
                return line.substring(GUID_LINE_PREFIX.length());
            }
        }
        throw new RuntimeException(tarEntry.getName() + ": Couldn't find GUID among the "+ lines.size() +" lines: " + lines);
    }

    private static String readFirstLine(TarArchiveEntry tarEntry, InputStream inputStream) {
        List<String> lines = readLines(inputStream);
        if (lines.isEmpty()) {
            throw new RuntimeException(tarEntry.getName() + ": File is empty");
        }

        if (lines.size() == 2 && "00".equals(lines.get(1))) {
            // Sometimes there's a second line with "00"?
        } else if (lines.size() != 1) {
            throw new RuntimeException(tarEntry.getName() + ": File expected to have 1 line, but found " + lines.size() + " lines: " + lines);
        }

        return lines.get(0);
    }

    private static List<String> readLines(InputStream inputStream) {

        List<String> lines = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.toList());
        return lines;
    }

}
