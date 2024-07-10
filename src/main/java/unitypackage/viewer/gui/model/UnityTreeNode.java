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

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import unitypackage.model.UnityAsset;

public abstract class UnityTreeNode extends DefaultMutableTreeNode implements Comparable<UnityTreeNode> {

    protected UnityTreeNode(Path userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }
    protected UnityTreeNode(UnityAsset userObject, boolean allowsChildren) {
        super(userObject, allowsChildren);
    }

    @Override
    public int compareTo(UnityTreeNode o) {

        int compare;
        if ((this instanceof Directory && o instanceof Directory) ||
            (this instanceof Asset && o instanceof Asset))
        {
            String thisVal = getStringForSearchingAndSorting().toLowerCase();
            String thatVal = o.getStringForSearchingAndSorting().toLowerCase();
            compare = thisVal.compareTo(thatVal);
        } else if (this instanceof Directory) {
            compare = -1;
        } else {
            compare = 1;
        }

        return compare;
    }



    abstract public String getAssetName();

    abstract public boolean hasGuid();
    abstract public String getGuid();

    abstract public boolean hasSize();
    abstract public long getAssetSize();

    abstract public BufferedImage getPreviewImage();

    abstract public String getStringForSearchingAndSorting();

    // ...................................................................................

    static class Directory extends UnityTreeNode {

        private final Path relativePathFromParent;

        private UnityAsset asset;

        public Directory(Path relativePathFromParent) {
            super(relativePathFromParent.getFileName(), true);
            this.relativePathFromParent = relativePathFromParent;
        }

        public void setAsset(UnityAsset asset) {
            this.asset = asset;
        }

        @Override
        public String getAssetName() {
            return relativePathFromParent.toString();
        }

        @Override
        public boolean hasGuid() {
            return getGuid() != null;
        }

        @Override
        public String getGuid() {
            String guid;
            if (asset == null) {
                guid = null;
            } else {
                guid = asset.getGuid();
            }
            return guid;
        }

        @Override
        public boolean hasSize() {
            return false;
        }

        @Override
        public long getAssetSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public BufferedImage getPreviewImage() {
            return null;
        }

        @Override
        public String getStringForSearchingAndSorting() {
            String s = relativePathFromParent.toString();
            String guid = getGuid();
            if (guid == null) {
                return s;
            } else {
                return s + "\t" + guid;
            }
        }

        public void recursiveSort() {
            if (this.children ==  null)
                return;
            this.children.sort(null);

            Enumeration<TreeNode> em = children();
            while (em.hasMoreElements()) {
                Object element = em.nextElement();
                if (element instanceof Directory) {
                    ((Directory)element).recursiveSort();
                }
            }

        }

        public boolean startsWith(Path relativePathFromOtherParent) {
            return relativePathFromParent.startsWith(relativePathFromOtherParent);
        }

        /**
         * String used when displaying the node in the tree.
         */
        @Override
        public String toString() {
            final Path fileName = relativePathFromParent.getFileName();

            if (fileName == null) { // must be the root node
                if (asset != null)
                    throw new RuntimeException("Can this happen?");
                return "<root>";
            }

            StringBuilder sb = new StringBuilder();

            sb.append("<html>")
              .append(fileName);
            if (asset != null) {
                sb.append(" <i><font color=#bbbbbb>{").append(asset.getGuid()).append("}</font></i>");
            } else {
                // I've seen some .unitypackage where directories get their own GUID and associated data.
                // I've also seen some where directories have no GUID, but are implied to exist by the asset paths.
                sb.append(" <i><font color=#ffaaaa>{missing directory .meta}</font></i>");
            }

            sb.append("</html>");
            return sb.toString();
        }
    }

    // ...................................................................................

    public static class Asset extends UnityTreeNode {

        public Asset(UnityAsset userObject) {
            super(userObject, false);
        }

        public UnityAsset getAsset() {
            return (UnityAsset) this.userObject;
        }

        @Override
        public String getAssetName() {
            return getAsset().getFileName();
        }

        @Override
        public boolean hasGuid() {
            return true;
        }

        @Override
        public String getGuid() {
            return getAsset().getGuid();
        }

        @Override
        public boolean hasSize() {
            return true;
        }

        @Override
        public long getAssetSize() {
            return getAsset().getSize();
        }

        @Override
        public BufferedImage getPreviewImage() {
            return getAsset().getPreview();
        }

        public String getAssetPath() {
            return getAsset().getFullPath();
        }

        public String getDateModified() {
            SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yy");
            String s = fmt.format(getAsset().getDateModified());
            return s;
        }

        @Override
        public String getStringForSearchingAndSorting() {
            return getAssetName()+ "\t" + getGuid();
        }

        /**
         * String used when displaying the node in the tree.
         */
        @Override
        public String toString() {
            String s = String.format("<html>%s <font color=#bbbbbb>(%,d bytes) <i>{%s}</i> %s</font></html>",
                                     getAssetName(), getAssetSize(), getGuid(), getDateModified());
            return s;
        }

    }
}
