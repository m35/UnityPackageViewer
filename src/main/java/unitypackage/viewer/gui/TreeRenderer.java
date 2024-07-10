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

import unitypackage.viewer.gui.model.UnityTreeNode;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

public class TreeRenderer extends DefaultTreeCellRenderer {

    private ImageIcon hasPreviewIcon;

    public TreeRenderer() {
        try (InputStream stream = getClass().getResourceAsStream("zoom-fit-best-2.png")) {
            if (stream != null) {
                hasPreviewIcon = new ImageIcon(ImageIO.read(stream));
            }
        } catch (IOException ex) {
            System.out.println("Error loading resource " + ex.getMessage());
        }
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

        Icon leafIcon = getDefaultLeafIcon();

        UnityTreeNode unityNode = (UnityTreeNode) value;
        BufferedImage preview = unityNode.getPreviewImage();
        if (preview != null) {
            leafIcon = hasPreviewIcon;
        }

        setLeafIcon(leafIcon);

        return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
    }
}
