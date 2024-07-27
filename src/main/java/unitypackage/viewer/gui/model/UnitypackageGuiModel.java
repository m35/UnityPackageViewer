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

package unitypackage.viewer.gui.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import unitypackage.model.UnityAsset;
import unitypackage.model.UnityPackage;

public class UnitypackageGuiModel {

    private UnityPackage currentUnitypackage;

    public File getCurrentUnitypackage() {
        return currentUnitypackage.getUnitypackageFile();
    }

    public void extractFile(UnityAsset asset, Path outputFile) throws IOException {
        try (InputStream is = currentUnitypackage.getFileStream(asset)) {
            Files.copy(is, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // ===================================================================================
    // Search model

    // TODO also search directories
    private final List<UnityTreeNode.Asset> assetNodesForSearching = new ArrayList<>();

    public List<SearchResult> search(String searchText) {


        List<SearchResult> searchResults = new ArrayList<>();

        for (UnityTreeNode.Asset pathsInPathname : assetNodesForSearching) {
            if (pathsInPathname.getStringForSearchingAndSorting().toLowerCase().contains(searchText.toLowerCase())) {
                searchResults.add(new SearchResult(pathsInPathname));
            }
        }

        Collections.sort(searchResults);

        return searchResults;
    }

    // ===================================================================================
    // Tree model

    private static final UnityAssetCompare unityAssetCompare = new UnityAssetCompare();
    private static class UnityAssetCompare implements Comparator<UnityAsset> {
        @Override
        public int compare(UnityAsset o1, UnityAsset o2) {
            return o1.getFullPath().compareTo(o2.getFullPath());
        }
    }

    /**
     * @param unitypackagePath Path to the ".unitypackage" file.
     */
    public DefaultTreeModel buildTreeModel(File unitypackagePath) throws IOException {

        currentUnitypackage = new UnityPackage(unitypackagePath);

        List<UnityAsset> unityAssets = currentUnitypackage.getUnityAssetList();

        UnityTreeNode.Directory root = new UnityTreeNode.Directory(Paths.get("(root)"));

        Map<Boolean, List<UnityAsset>> split = unityAssets.stream().collect(Collectors.groupingBy(a -> a.isProbablyDirectory()));
        List<UnityAsset> dirEntries = split.get(true);
        List<UnityAsset> fileEntries = split.get(false);
        fileEntries.sort(unityAssetCompare);

        if (dirEntries != null) {
            // I suppose it's possible for no directories to exist
            dirEntries.sort(unityAssetCompare);

            // Build up a directory tree of the identified directory assets
            for (UnityAsset directoryAssetEntry : dirEntries) {
                UnityTreeNode.Directory currentDirectory = root;
                Path dirPath = directoryAssetEntry.getFullPathAsPath();

                for (int i = 0; i < dirPath.getNameCount(); i++) {
                    Path pathPart = dirPath.getName(i);
                    currentDirectory = findOrCreateDirectoryNode(currentDirectory, pathPart);
                }

                currentDirectory.setAsset(directoryAssetEntry);
            }
        }

        // Now add the file assets as children in the tree.
        // If somehow there's a missing directory, it will create it.
        for (UnityAsset asset : fileEntries) {

            UnityTreeNode.Directory currentDir = root;
            Path fileAssetPath = asset.getFullPathAsPath();

            for (int i = 0; i < fileAssetPath.getNameCount()-1; i++) {
                Path pathPart = fileAssetPath.getName(i);
                currentDir = findOrCreateDirectoryNode(currentDir, pathPart);
            }

            UnityTreeNode.Asset assetNode = new UnityTreeNode.Asset(asset);
            currentDir.add(assetNode);
            assetNodesForSearching.add(assetNode);
        }

        root.recursiveSort();
        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        return treeModel;
    }

    private static UnityTreeNode.Directory findOrCreateDirectoryNode(UnityTreeNode parent, Path relativePathFromParent) {

        @SuppressWarnings("unchecked")
        Enumeration<TreeNode> kids = parent.children();
        while (kids.hasMoreElements()) {
            Object nextKid = kids.nextElement();

            if (nextKid instanceof UnityTreeNode.Asset) {
                continue;
            }

            UnityTreeNode.Directory dn = (UnityTreeNode.Directory)nextKid;
            if (dn.startsWith(relativePathFromParent)) {
                return dn;
            }
        }

        UnityTreeNode.Directory newNode = new UnityTreeNode.Directory(relativePathFromParent);
        parent.add(newNode);

        return newNode;
    }

}
