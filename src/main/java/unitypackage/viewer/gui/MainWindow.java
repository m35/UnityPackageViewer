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

import java.awt.Color;
import unitypackage.viewer.gui.model.UnityTreeNode;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import unitypackage.model.UnityAsset;
import unitypackage.viewer.Main;
import unitypackage.viewer.gui.model.SearchResult;
import unitypackage.viewer.gui.model.UnitypackageGuiModel;


public class MainWindow extends JFrame {

    private final UnitypackageGuiModel guiModel = new UnitypackageGuiModel();


    private final DropTarget thisDropTarget = new DropTarget() {
        @Override
        public synchronized void drop(DropTargetDropEvent evt) {
            try {
                evt.acceptDrop(DnDConstants.ACTION_COPY);
                List<File> droppedFiles = (List<File>)evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                if (droppedFiles.size() == 1) {
                    File f = droppedFiles.get(0);
                    if (UnitypackageFileName.isUnitypackage(f)) {
                        openFile(f);
                    } else {
                        System.out.println("Weird file name " + droppedFiles);
                    }
                } else {
                    System.out.println("Weird # of files " + droppedFiles);
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                ex.printStackTrace();
            } finally {
                evt.dropComplete(true);
            }
        }
    };

    private final LoadingGlassPane loadingGlassPane = new LoadingGlassPane();
    private static class LoadingGlassPane extends JLabel {

        public LoadingGlassPane() {
            super("Loading ...");
            setOpaque(true);
            setBackground(new Color(255, 255, 255, 128));
            setHorizontalAlignment(SwingConstants.CENTER);

            // Add noop input handling to block all events
            addMouseListener(new MouseAdapter() {});
            addKeyListener(new KeyAdapter() {});
            addMouseMotionListener(new MouseMotionAdapter() {});
        }
    }

    public MainWindow(String fileToOpen) {
        // Use the system's L&F if available
        super(makeWindowsTitle(null));
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        Image icon16 = Toolkit.getDefaultToolkit().createImage(MainWindow.class.getResource("Icon16.png"));
        Image icon32 = Toolkit.getDefaultToolkit().createImage(MainWindow.class.getResource("Icon32.png"));
        Image icon64 = Toolkit.getDefaultToolkit().createImage(MainWindow.class.getResource("Icon64.png"));
        Image icon128 = Toolkit.getDefaultToolkit().createImage(MainWindow.class.getResource("Icon128.png"));
        Image icon256 = Toolkit.getDefaultToolkit().createImage(MainWindow.class.getResource("Icon256.png"));
        setIconImages(Arrays.asList(icon16, icon32, icon64, icon128, icon256));

        setSize(500, 500);
        setMinimumSize(new Dimension(800, 600));

        initComponents();

        guiPackageTree.setRowHeight(guiPackageTree.getRowHeight() + 5);
        guiPackageTree.setCellRenderer(new TreeRenderer());

        setGlassPane(loadingGlassPane);
        setLocationRelativeTo(null); // center window

        setDropTarget(thisDropTarget);

        // The split ends up too small, can't find why, so just adjust it manually
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                // Seems you can't set a percentage of split position until after it has been displayed
                guiMainSplitPane.setDividerLocation(0.7);
                removeComponentListener(this);
            }
        });

        if (fileToOpen != null) {
            File file = new File(fileToOpen);
            if (UnitypackageFileName.isUnitypackage(file)) {
                openFile(file);
            }
        }
    }

    private static String makeWindowsTitle(String fileName) {
        String title = "Basic .unitypackage Viewer " + Main.VERSION;

        if (fileName != null) {
            title = fileName + " - " + title;
        }

        return title;
    }

    private void setWindowTitleFile(String fileName) {
        setTitle(makeWindowsTitle(fileName));
    }


    private void openFile(File unitypackagePath) {

        SwingWorker<DefaultTreeModel, Void> swingWorker = new SwingWorker<DefaultTreeModel, Void>() {

            @Override
            protected DefaultTreeModel doInBackground() throws Exception {
                DefaultTreeModel uiTreeModel = guiModel.buildTreeModel(unitypackagePath);
                HistoryIni.addLastFile(unitypackagePath);
                return uiTreeModel;
            }

            @Override
            protected void done() {

                Exception thrown = null;
                try {
                    DefaultTreeModel uiTreeModel = get();
                    guiPackageTree.setModel(uiTreeModel);
                    guiCurrentFileLabel.setText(unitypackagePath.toString());
                    expandAllTree();
                    setWindowTitleFile(unitypackagePath.getName());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    thrown = ex;
                } finally {
                    loadingGlassPane.setVisible(false);
                    setCursor(Cursor.getDefaultCursor());
                }

                if (thrown != null) {
                    JOptionPane.showMessageDialog(MainWindow.this, "Error opening file " + thrown.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        // Clear any search results
        guiTextSearch.setText("");
        DefaultListModel<SearchResult> listModel = (DefaultListModel<SearchResult>) guiListResults.getModel();
        listModel.clear();

        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        loadingGlassPane.setText("Loading " + unitypackagePath + " ...");
        loadingGlassPane.setVisible(true);

        swingWorker.execute();
    }

    private void expandAllTree() {
        for (int i = 0; i < guiPackageTree.getRowCount(); i++) {
            guiPackageTree.expandRow(i);
        }
    }
    private void collapseAllTree() {
        for (int i = guiPackageTree.getRowCount()-1; i > 0; i--) {
            guiPackageTree.collapseRow(i);
        }
    }

    private UnityTreeNode getSingleSelectedTreeNode() {

        UnityTreeNode selectedNode;

        TreeSelectionModel model = guiPackageTree.getSelectionModel();
        if (model.getSelectionCount() == 1) {
            selectedNode = (UnityTreeNode)model.getSelectionPath().getLastPathComponent();
        } else {
            selectedNode = null;
        }

        return selectedNode;
    }

    private void setClipboard(String text) {
        Toolkit.getDefaultToolkit()
                .getSystemClipboard()
                .setContents(new StringSelection(text), null);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        guiTreePopupMenu = new javax.swing.JPopupMenu();
        guiTreeMenuCopyName = new javax.swing.JMenuItem();
        guiTreeMenuCopySize = new javax.swing.JMenuItem();
        guiTreeMenuCopyGuid = new javax.swing.JMenuItem();
        guiOpenButton = new javax.swing.JButton();
        guiCurrentFileLabel = new javax.swing.JLabel();
        guiMainSplitPane = new javax.swing.JSplitPane();
        guiTopPanel = new javax.swing.JPanel();
        guiPackageTreeScrollPane = new javax.swing.JScrollPane();
        guiPackageTree = new javax.swing.JTree();
        guiRightPanel = new javax.swing.JPanel();
        guiCollapseAllButton = new javax.swing.JButton();
        guiExpandAllButton = new javax.swing.JButton();
        guiPreviewLabelImage = new javax.swing.JLabel();
        guiExportButton = new javax.swing.JButton();
        guiBottomPanel = new javax.swing.JPanel();
        guiTextSearch = new javax.swing.JTextField();
        guiListResultsScrollPane = new javax.swing.JScrollPane();
        guiListResults = new javax.swing.JList<>();
        guiSearchButton = new javax.swing.JButton();

        guiTreePopupMenu.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                guiTreePopupMenuPopupMenuWillBecomeVisible(evt);
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
        });

        guiTreeMenuCopyName.setText("Copy file name");
        guiTreeMenuCopyName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiTreeMenuCopyNameActionPerformed(evt);
            }
        });
        guiTreePopupMenu.add(guiTreeMenuCopyName);

        guiTreeMenuCopySize.setText("Copy size");
        guiTreeMenuCopySize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiTreeMenuCopySizeActionPerformed(evt);
            }
        });
        guiTreePopupMenu.add(guiTreeMenuCopySize);

        guiTreeMenuCopyGuid.setText("Copy guid");
        guiTreeMenuCopyGuid.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiTreeMenuCopyGuidActionPerformed(evt);
            }
        });
        guiTreePopupMenu.add(guiTreeMenuCopyGuid);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        guiOpenButton.setText("Open...");
        guiOpenButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiOpenButtonActionPerformed(evt);
            }
        });
        getContentPane().add(guiOpenButton, java.awt.BorderLayout.NORTH);

        guiCurrentFileLabel.setText(".");
        getContentPane().add(guiCurrentFileLabel, java.awt.BorderLayout.SOUTH);

        guiMainSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        guiMainSplitPane.setResizeWeight(1.0);

        guiTopPanel.setLayout(new java.awt.BorderLayout());

        guiPackageTree.setModel(null);
        guiPackageTree.setRootVisible(false);
        guiPackageTree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                guiPackageTreeMouseClicked(evt);
            }
        });
        guiPackageTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                guiPackageTreeValueChanged(evt);
            }
        });
        guiPackageTreeScrollPane.setViewportView(guiPackageTree);

        guiTopPanel.add(guiPackageTreeScrollPane, java.awt.BorderLayout.CENTER);

        guiRightPanel.setLayout(new java.awt.GridBagLayout());

        guiCollapseAllButton.setText("Collapse all");
        guiCollapseAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiCollapseAllButtonActionPerformed(evt);
            }
        });
        guiRightPanel.add(guiCollapseAllButton, new java.awt.GridBagConstraints());

        guiExpandAllButton.setText("Expand all");
        guiExpandAllButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiExpandAllButtonActionPerformed(evt);
            }
        });
        guiRightPanel.add(guiExpandAllButton, new java.awt.GridBagConstraints());

        guiPreviewLabelImage.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        guiPreviewLabelImage.setBorder(javax.swing.BorderFactory.createTitledBorder("Preview"));
        guiPreviewLabelImage.setIconTextGap(0);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.RELATIVE;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 10, 10, 10);
        guiRightPanel.add(guiPreviewLabelImage, gridBagConstraints);

        guiExportButton.setText("Export");
        guiExportButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiExportButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        guiRightPanel.add(guiExportButton, gridBagConstraints);

        guiTopPanel.add(guiRightPanel, java.awt.BorderLayout.LINE_END);

        guiMainSplitPane.setTopComponent(guiTopPanel);

        guiBottomPanel.setLayout(new java.awt.BorderLayout());

        guiTextSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiTextSearchActionPerformed(evt);
            }
        });
        guiBottomPanel.add(guiTextSearch, java.awt.BorderLayout.NORTH);

        guiListResults.setModel(new DefaultListModel<>());
        guiListResults.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        guiListResults.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                guiListResultsValueChanged(evt);
            }
        });
        guiListResultsScrollPane.setViewportView(guiListResults);

        guiBottomPanel.add(guiListResultsScrollPane, java.awt.BorderLayout.CENTER);

        guiSearchButton.setText("Search");
        guiSearchButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                guiSearchButtonActionPerformed(evt);
            }
        });
        guiBottomPanel.add(guiSearchButton, java.awt.BorderLayout.EAST);

        guiMainSplitPane.setBottomComponent(guiBottomPanel);

        getContentPane().add(guiMainSplitPane, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void guiSearchButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiSearchButtonActionPerformed

        TreeSelectionModel treeSelectionModel = guiPackageTree.getSelectionModel();
        treeSelectionModel.clearSelection();

        DefaultListModel<SearchResult> listModel = (DefaultListModel<SearchResult>) guiListResults.getModel();
        listModel.clear();

        String searchText = guiTextSearch.getText();
        if (searchText.isEmpty()) {
            return;
        }

        List<SearchResult> matches = guiModel.search(searchText);

        for (SearchResult listItem : matches) {
            listModel.addElement(listItem);

            TreePath tp = new TreePath(listItem.getTreeNodePath());
            treeSelectionModel.addSelectionPath(tp);
        }

    }//GEN-LAST:event_guiSearchButtonActionPerformed

    private void guiTextSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiTextSearchActionPerformed
        guiSearchButtonActionPerformed(evt);
    }//GEN-LAST:event_guiTextSearchActionPerformed

    private void guiExportButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiExportButtonActionPerformed

        TreePath tp = guiPackageTree.getSelectionPath();
        if (tp == null) {
            // Somehow nothing is selected
            return;
        }
        UnityTreeNode.Asset assetNode = (UnityTreeNode.Asset) tp.getLastPathComponent(); // assumes no empty directories
        UnityAsset asset = assetNode.getAsset();
        String assetFileName = asset.getFileName();

        Path outputFile = guiModel.getCurrentUnitypackageFile().toPath().getParent().resolve(assetFileName);
        boolean exists = Files.exists(outputFile);
        if (exists) {
            int dialogResult = JOptionPane.showConfirmDialog(null,
                    "Overwrite existing file " + outputFile + " ?","Overwrite?",
                    JOptionPane.YES_NO_OPTION);
            if (dialogResult != JOptionPane.YES_OPTION){
              return;
            }
        }

        try {
            guiModel.extractFile(asset, outputFile);

            // Open folder containing the extracted file
            boolean USE_DESKTOP = true;
            if (USE_DESKTOP) {
                // Opens the directory window if it doesn't exist (Windows behavior)
                // Does not select the extracted file.
                // TODO I think Java 9 has some better APIs for this.
                Desktop.getDesktop().browse(outputFile.getParent().toUri());
            } else {
                // Windows specific, highlights the extracted file, but also opens up a new window every time
                Runtime.getRuntime().exec("explorer.exe /select," + outputFile);
            }

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error writing file " + outputFile + " " + ex.getMessage());
        }

    }//GEN-LAST:event_guiExportButtonActionPerformed

    private void guiListResultsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_guiListResultsValueChanged
        SearchResult selection = guiListResults.getSelectedValue();
        if (selection == null) {
            return;
        }

        TreePath tp = new TreePath(selection.getTreeNodePath());

        guiPackageTree.scrollPathToVisible(tp);
        guiPackageTree.setSelectionPath(tp);

    }//GEN-LAST:event_guiListResultsValueChanged

    private void guiOpenButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiOpenButtonActionPerformed

        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(true);
        fc.setDialogTitle(".unitypackage");
        UnitypackageFileName.UnitypackageFileFilter fileFilter = new UnitypackageFileName.UnitypackageFileFilter();
        fc.addChoosableFileFilter(fileFilter);
        fc.setFileFilter(fileFilter);

        File lastDir = HistoryIni.getLastDirectory();
        if (lastDir != null) {
            fc.setCurrentDirectory(lastDir);
        }

        int result = fc.showOpenDialog(this);
        if (result != JFileChooser.APPROVE_OPTION)
            return;
        File f = fc.getSelectedFile();
        openFile(f);

    }//GEN-LAST:event_guiOpenButtonActionPerformed

    private void guiExpandAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiExpandAllButtonActionPerformed
        expandAllTree();
    }//GEN-LAST:event_guiExpandAllButtonActionPerformed

    private void guiCollapseAllButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiCollapseAllButtonActionPerformed
        collapseAllTree();
    }//GEN-LAST:event_guiCollapseAllButtonActionPerformed

    private void guiPackageTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_guiPackageTreeValueChanged

        Icon previewIcon = null; // null clears the image

        UnityTreeNode selectedNode = getSingleSelectedTreeNode();

        if (selectedNode != null) {
            BufferedImage preview = selectedNode.getPreviewImage();
            if (preview != null)
                previewIcon = new ImageIcon(preview);
        }

        guiPreviewLabelImage.setIcon(previewIcon);

    }//GEN-LAST:event_guiPackageTreeValueChanged


    private void guiPackageTreeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_guiPackageTreeMouseClicked

        if (SwingUtilities.isRightMouseButton(evt)) {

            int row = guiPackageTree.getClosestRowForLocation(evt.getX(), evt.getY());
            guiPackageTree.setSelectionRow(row);
            guiTreePopupMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }

    }//GEN-LAST:event_guiPackageTreeMouseClicked

    private void guiTreeMenuCopyNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiTreeMenuCopyNameActionPerformed

        UnityTreeNode selection = getSingleSelectedTreeNode();
        if (selection != null) {
            setClipboard(selection.getAssetName());
        }

    }//GEN-LAST:event_guiTreeMenuCopyNameActionPerformed

    private void guiTreeMenuCopySizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiTreeMenuCopySizeActionPerformed

        UnityTreeNode selection = getSingleSelectedTreeNode();
        if (selection != null) {
            setClipboard(String.valueOf(selection.getAssetSize()));
        }

    }//GEN-LAST:event_guiTreeMenuCopySizeActionPerformed

    private void guiTreeMenuCopyGuidActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_guiTreeMenuCopyGuidActionPerformed

        UnityTreeNode selection = getSingleSelectedTreeNode();
        if (selection != null) {
            String guid = selection.getGuid();
            if (guid != null) {
                setClipboard(guid);
            }
        }

    }//GEN-LAST:event_guiTreeMenuCopyGuidActionPerformed

    private void guiTreePopupMenuPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_guiTreePopupMenuPopupMenuWillBecomeVisible

        UnityTreeNode node = getSingleSelectedTreeNode();
        if (node == null) {
            return;
        }
        guiTreeMenuCopyGuid.setVisible(node.hasGuid());
        guiTreeMenuCopySize.setVisible(node.hasSize());

    }//GEN-LAST:event_guiTreePopupMenuPopupMenuWillBecomeVisible


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel guiBottomPanel;
    private javax.swing.JButton guiCollapseAllButton;
    private javax.swing.JLabel guiCurrentFileLabel;
    private javax.swing.JButton guiExpandAllButton;
    private javax.swing.JButton guiExportButton;
    private javax.swing.JList<SearchResult> guiListResults;
    private javax.swing.JScrollPane guiListResultsScrollPane;
    private javax.swing.JSplitPane guiMainSplitPane;
    private javax.swing.JButton guiOpenButton;
    private javax.swing.JTree guiPackageTree;
    private javax.swing.JScrollPane guiPackageTreeScrollPane;
    private javax.swing.JLabel guiPreviewLabelImage;
    private javax.swing.JPanel guiRightPanel;
    private javax.swing.JButton guiSearchButton;
    private javax.swing.JTextField guiTextSearch;
    private javax.swing.JPanel guiTopPanel;
    private javax.swing.JMenuItem guiTreeMenuCopyGuid;
    private javax.swing.JMenuItem guiTreeMenuCopyName;
    private javax.swing.JMenuItem guiTreeMenuCopySize;
    private javax.swing.JPopupMenu guiTreePopupMenu;
    // End of variables declaration//GEN-END:variables


}
