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

import javax.swing.tree.TreeNode;

public class SearchResult implements Comparable<SearchResult> {

    private final UnityTreeNode.Asset assetNode;

    public SearchResult(UnityTreeNode.Asset assetNode) {
        this.assetNode = assetNode;
    }

    public TreeNode[] getTreeNodePath() {
        return assetNode.getPath();
    }


    @Override
    public int compareTo(SearchResult o) {
        return toString().toLowerCase().compareTo(o.toString().toLowerCase());
    }

    /**
     * String used when displaying an item in the list.
     */
    @Override
    public String toString() {
        String s = String.format("%s (%,d bytes) {%s}", assetNode.getAssetPath(), assetNode.getAssetSize(), assetNode.getGuid());
        return s;
    }
}
