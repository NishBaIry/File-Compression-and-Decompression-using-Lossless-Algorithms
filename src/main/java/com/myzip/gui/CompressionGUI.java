package com.myzip.gui;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.util.*;
import java.util.List;
import com.myzip.core.ArchiveManager;
import com.formdev.flatlaf.*;

/**
 * WinZip/WinRAR Style GUI for File Compression System
 * Enhanced with drag-and-drop, cancelable operations, and improved feedback
 */
public class CompressionGUI extends JFrame {

    // UI Components
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextField addressBar;
    private JLabel statusLabel;
    private JLabel sizeLabel;
    private JProgressBar progressBar;
    private JTextArea logArea;
    private JDialog logDialog;

    // Toolbar buttons
    private JButton compressBtn;
    private JButton extractBtn;
    private JButton viewBtn;
    private JButton deleteBtn;
    private JButton infoBtn;

    // State
    private File currentArchive = null;
    private List<ArchiveManager.RestoredFile> currentFiles = new ArrayList<>();

    // Cancelable operation support
    private volatile boolean operationCancelled = false;
    private SwingWorker<?, ?> currentWorker = null;

    public CompressionGUI() {
        setTitle("MyZip - File Compression Utility");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Set application icon
        setAppIcon();

        initComponents();
        setupDragAndDrop();
        updateStatus("Ready");
    }

    /**
     * Set the application icon for the window
     */
    private void setAppIcon() {
        try {
            java.net.URL iconURL = getClass().getResource("/icons/app-icon.png");
            if (iconURL != null) {
                ImageIcon icon = new ImageIcon(iconURL);
                setIconImage(icon.getImage());
            } else {
                System.err.println("App icon not found at /icons/app-icon.png");
            }
        } catch (Exception e) {
            System.err.println("Error loading app icon: " + e.getMessage());
        }
    }

    /**
     * Load icon from resources folder (WinZip style - larger icons)
     */
    private ImageIcon loadIcon(String filename) {
        try {
            java.net.URL iconURL = getClass().getResource("/icons/" + filename);
            if (iconURL != null) {
                ImageIcon icon = new ImageIcon(iconURL);
                // Scale to 48x48 for WinZip-style prominent icons
                Image img = icon.getImage().getScaledInstance(48, 48, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            } else {
                System.err.println("Icon not found: /icons/" + filename);
            }
        } catch (Exception e) {
            System.err.println("Error loading icon '" + filename + "': " + e.getMessage());
        }
        return null;
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        // Initialize log area (but don't show it yet)
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 11));
        logArea.setBackground(new Color(252, 252, 252));

        // Menu Bar
        setJMenuBar(createMenuBar());

        // Main toolbar
        add(createToolBar(), BorderLayout.NORTH);

        // Center - File table panel (no split pane)
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(createAddressBar(), BorderLayout.NORTH);
        tablePanel.add(createFileTable(), BorderLayout.CENTER);
        add(tablePanel, BorderLayout.CENTER);

        // Status bar
        add(createStatusBar(), BorderLayout.SOUTH);
    }

    /**
     * Setup drag-and-drop support for files and folders
     */
    private void setupDragAndDrop() {
        // Create drop target for the entire window
        new DropTarget(this, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);

                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) dtde.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);

                    if (!droppedFiles.isEmpty()) {
                        handleDroppedFiles(droppedFiles);
                    }

                    dtde.dropComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    dtde.dropComplete(false);
                }
            }

            @Override
            public void dragEnter(DropTargetDragEvent dtde) {
                // Visual feedback when dragging over
                getContentPane().setBackground(new Color(230, 240, 250));
            }

            @Override
            public void dragExit(DropTargetEvent dte) {
                // Reset background when drag leaves
                getContentPane().setBackground(null);
            }
        });

        // Also add drop target to file table for better UX
        new DropTarget(fileTable, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);

                    @SuppressWarnings("unchecked")
                    List<File> droppedFiles = (List<File>) dtde.getTransferable()
                        .getTransferData(DataFlavor.javaFileListFlavor);

                    if (!droppedFiles.isEmpty()) {
                        handleDroppedFiles(droppedFiles);
                    }

                    dtde.dropComplete(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    dtde.dropComplete(false);
                }
            }
        });
    }

    /**
     * Handle files dropped via drag-and-drop
     */
    private void handleDroppedFiles(List<File> droppedFiles) {
        // Check if any dropped file is a .myzip archive
        boolean hasArchive = droppedFiles.stream()
            .anyMatch(f -> f.getName().toLowerCase().endsWith(".myzip"));

        if (hasArchive && droppedFiles.size() == 1) {
            // Single .myzip file - open it
            File archive = droppedFiles.get(0);
            logSuccess("Dropped archive detected: " + archive.getName() + "\n");
            loadArchive(archive);
        } else {
            // Files/folders to compress
            logInfo("Files dropped for compression (" + droppedFiles.size() + " items)\n");
            compressDroppedFiles(droppedFiles);
        }
    }

    /**
     * Compress files that were dropped
     */
    private void compressDroppedFiles(List<File> droppedFiles) {
        // Choose save location
        JFileChooser saveChooser = new JFileChooser();
        saveChooser.setDialogTitle("Save Archive As");
        saveChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        saveChooser.setSelectedFile(new File(saveChooser.getCurrentDirectory(), "archive.myzip"));

        if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File outputFile = saveChooser.getSelectedFile();

            if (outputFile.isDirectory()) {
                outputFile = new File(outputFile, "archive.myzip");
            } else if (!outputFile.getName().endsWith(".myzip")) {
                outputFile = new File(outputFile.getAbsolutePath() + ".myzip");
            }

            performCompression(droppedFiles.toArray(new File[0]), outputFile);
        }
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem newArchive = new JMenuItem("New Archive...", KeyEvent.VK_N);
        newArchive.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newArchive.addActionListener(e -> compressFiles());

        JMenuItem openArchive = new JMenuItem("Open Archive...", KeyEvent.VK_O);
        openArchive.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        openArchive.addActionListener(e -> openArchive());

        JMenuItem closeArchive = new JMenuItem("Close Archive", KeyEvent.VK_C);
        closeArchive.addActionListener(e -> closeArchive());

        JMenuItem exit = new JMenuItem("Exit", KeyEvent.VK_X);
        exit.addActionListener(e -> System.exit(0));

        fileMenu.add(newArchive);
        fileMenu.add(openArchive);
        fileMenu.add(closeArchive);
        fileMenu.addSeparator();
        fileMenu.add(exit);
        menuBar.add(fileMenu);

        // Actions menu
        JMenu actionsMenu = new JMenu("Actions");
        actionsMenu.setMnemonic(KeyEvent.VK_A);

        JMenuItem extractAll = new JMenuItem("Extract All...", KeyEvent.VK_E);
        extractAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        extractAll.addActionListener(e -> extractArchive());

        JMenuItem showLog = new JMenuItem("Show Log", KeyEvent.VK_L);
        showLog.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        showLog.addActionListener(e -> showLogWindow());

        actionsMenu.add(extractAll);
        actionsMenu.addSeparator();
        actionsMenu.add(showLog);
        menuBar.add(actionsMenu);

        // Help menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem about = new JMenuItem("About MyZip", KeyEvent.VK_A);
        about.addActionListener(e -> showAbout());

        helpMenu.add(about);
        menuBar.add(helpMenu);

        return menuBar;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Compress button
        compressBtn = createToolButton("Compress", loadIcon("compress.png"), "Compress files/folders into archive (or drag & drop files here)", e -> compressFiles());
        toolBar.add(compressBtn);

        toolBar.add(Box.createHorizontalStrut(8));

        // Extract button
        extractBtn = createToolButton("Extract", loadIcon("extract.png"), "Extract files from archive", e -> extractArchive());
        toolBar.add(extractBtn);

        toolBar.add(Box.createHorizontalStrut(16));

        // View button
        viewBtn = createToolButton("View", loadIcon("view.png"), "View selected file", e -> viewSelectedFile());
        toolBar.add(viewBtn);

        toolBar.add(Box.createHorizontalStrut(8));

        // Delete button
        deleteBtn = createToolButton("Delete", loadIcon("delete.png"), "Delete selected files", e -> deleteSelectedFiles());
        toolBar.add(deleteBtn);

        toolBar.add(Box.createHorizontalStrut(16));

        // Info button
        infoBtn = createToolButton("Info", loadIcon("info.png"), "Archive information", e -> showArchiveInfo());
        toolBar.add(infoBtn);

        return toolBar;
    }

    private JButton createToolButton(String text, ImageIcon icon, String tooltip, ActionListener action) {
        JButton btn;
        if (icon != null) {
            btn = new JButton(text, icon);
            // WinZip style: Icon on top, text below
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setIconTextGap(4);
        } else {
            btn = new JButton(text);
        }

        btn.setToolTipText(tooltip);
        btn.setFocusPainted(false);
        btn.addActionListener(action);
        return btn;
    }

    private JPanel createAddressBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel label = new JLabel("📁 Archive:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(60, 70, 90));
        panel.add(label, BorderLayout.WEST);

        addressBar = new JTextField();
        addressBar.setEditable(false);
        addressBar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        addressBar.setForeground(new Color(80, 80, 80));
        addressBar.setBackground(Color.WHITE);
        addressBar.setText("No archive open - Drag & drop files to compress, or use 'Compress' button");
        addressBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235), 1),
            new EmptyBorder(6, 10, 6, 10)
        ));
        panel.add(addressBar, BorderLayout.CENTER);

        JButton browseBtn = new JButton("Browse");
        browseBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        browseBtn.setPreferredSize(new Dimension(80, 32));
        browseBtn.setBackground(new Color(240, 245, 250));
        browseBtn.setForeground(new Color(60, 70, 90));
        browseBtn.setFocusPainted(false);
        browseBtn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235), 1),
            new EmptyBorder(4, 12, 4, 12)
        ));
        browseBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Hover effect
        browseBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                browseBtn.setBackground(new Color(230, 238, 248));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                browseBtn.setBackground(new Color(240, 245, 250));
            }
        });

        browseBtn.addActionListener(e -> openArchive());
        panel.add(browseBtn, BorderLayout.EAST);

        return panel;
    }

    private JScrollPane createFileTable() {
        String[] columns = {"Name", "Size", "Compressed", "Type", "Modified"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        fileTable.setRowHeight(28);
        fileTable.setShowGrid(false);
        fileTable.setIntercellSpacing(new Dimension(0, 0));
        fileTable.getTableHeader().setReorderingAllowed(false);
        fileTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        // Modern header styling
        JTableHeader header = fileTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(240, 245, 250));
        header.setForeground(new Color(60, 70, 90));
        header.setPreferredSize(new Dimension(header.getWidth(), 32));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(220, 225, 235)));

        // Column widths
        fileTable.getColumnModel().getColumn(0).setPreferredWidth(300);
        fileTable.getColumnModel().getColumn(1).setPreferredWidth(80);
        fileTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        fileTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        fileTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        // Modern alternating row colors with hover effect
        fileTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(new Color(66, 135, 245));
                    c.setForeground(Color.WHITE);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    c.setForeground(new Color(60, 60, 60));
                    setFont(getFont().deriveFont(Font.PLAIN));
                }

                // Add padding
                setBorder(new EmptyBorder(4, 8, 4, 8));

                return c;
            }
        });

        // Add hover effect to table
        fileTable.addMouseMotionListener(new MouseAdapter() {
            private int lastRow = -1;

            @Override
            public void mouseMoved(MouseEvent e) {
                int row = fileTable.rowAtPoint(e.getPoint());
                if (row != lastRow) {
                    lastRow = row;
                    fileTable.repaint();
                }
            }
        });

        // Double-click to view
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelectedFile();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(fileTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        return scrollPane;
    }

    private void showLogWindow() {
        if (logDialog == null) {
            // Create modern log dialog
            logDialog = new JDialog(this, "Activity Log", false);
            logDialog.setSize(750, 500);
            logDialog.setLocationRelativeTo(this);

            JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
            mainPanel.setBackground(new Color(250, 252, 255));

            // Header
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(240, 248, 255));
            headerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(220, 230, 245)),
                new EmptyBorder(16, 20, 16, 20)
            ));

            JLabel titleLabel = new JLabel("📋 Operation Activity Log");
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            titleLabel.setForeground(new Color(60, 70, 90));
            headerPanel.add(titleLabel, BorderLayout.WEST);

            mainPanel.add(headerPanel, BorderLayout.NORTH);

            // Content panel
            JPanel contentPanel = new JPanel(new BorderLayout());
            contentPanel.setBackground(new Color(250, 252, 255));
            contentPanel.setBorder(new EmptyBorder(16, 16, 16, 16));

            // Log text area with modern styling
            JScrollPane scrollPane = new JScrollPane(logArea);
            scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 245), 2),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
            ));
            scrollPane.getViewport().setBackground(new Color(252, 252, 252));
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            mainPanel.add(contentPanel, BorderLayout.CENTER);

            // Buttons panel
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 16));
            buttonPanel.setBackground(new Color(250, 252, 255));

            JButton clearBtn = new JButton("Clear Log");
            clearBtn.setPreferredSize(new Dimension(100, 34));
            clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            clearBtn.setBackground(new Color(255, 152, 0));
            clearBtn.setForeground(Color.WHITE);
            clearBtn.setFocusPainted(false);
            clearBtn.setBorderPainted(false);
            clearBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            clearBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    clearBtn.setBackground(new Color(245, 135, 0));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    clearBtn.setBackground(new Color(255, 152, 0));
                }
            });
            clearBtn.addActionListener(e -> logArea.setText(""));

            JButton closeBtn = new JButton("Close");
            closeBtn.setPreferredSize(new Dimension(100, 34));
            closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            closeBtn.setBackground(new Color(66, 135, 245));
            closeBtn.setForeground(Color.WHITE);
            closeBtn.setFocusPainted(false);
            closeBtn.setBorderPainted(false);
            closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            closeBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    closeBtn.setBackground(new Color(50, 120, 230));
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    closeBtn.setBackground(new Color(66, 135, 245));
                }
            });
            closeBtn.addActionListener(e -> logDialog.setVisible(false));

            buttonPanel.add(clearBtn);
            buttonPanel.add(closeBtn);
            mainPanel.add(buttonPanel, BorderLayout.SOUTH);

            logDialog.add(mainPanel);
        }

        logDialog.setVisible(true);
        logDialog.toFront();
    }

    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new BorderLayout(10, 0));
        statusBar.setBackground(new Color(248, 250, 252));
        statusBar.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(220, 225, 235)),
            new EmptyBorder(8, 16, 8, 16)
        ));

        // Left panel with status indicator
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftPanel.setOpaque(false);

        // Status indicator (green dot)
        JLabel statusIndicator = new JLabel("●");
        statusIndicator.setFont(new Font("Segoe UI", Font.BOLD, 16));
        statusIndicator.setForeground(new Color(76, 175, 80));
        leftPanel.add(statusIndicator);

        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(60, 60, 60));
        leftPanel.add(statusLabel);

        statusBar.add(leftPanel, BorderLayout.WEST);

        // Right panel
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        rightPanel.setOpaque(false);

        progressBar = new JProgressBar(0, 100);
        progressBar.setPreferredSize(new Dimension(180, 20));
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setForeground(new Color(66, 135, 245));
        progressBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 225, 235)),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        rightPanel.add(progressBar);

        sizeLabel = new JLabel("0 files | 0 B");
        sizeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sizeLabel.setForeground(new Color(100, 100, 100));
        rightPanel.add(sizeLabel);

        statusBar.add(rightPanel, BorderLayout.EAST);

        return statusBar;
    }

    // ==================== Color-coded logging ====================

    private void logSuccess(String message) {
        appendColoredLog(message, new Color(0, 150, 0)); // Green
    }

    private void logWarning(String message) {
        appendColoredLog(message, new Color(200, 120, 0)); // Orange
    }

    private void logError(String message) {
        appendColoredLog(message, new Color(200, 0, 0)); // Red
    }

    private void logInfo(String message) {
        appendColoredLog(message, Color.BLACK); // Default
    }

    private void appendColoredLog(String message, Color color) {
        // For simplicity in plain JTextArea, we'll use prefixes
        String prefix = "";
        if (color.equals(new Color(0, 150, 0))) {
            prefix = "[✓] ";
        } else if (color.equals(new Color(200, 120, 0))) {
            prefix = "[!] ";
        } else if (color.equals(new Color(200, 0, 0))) {
            prefix = "[✗] ";
        } else {
            prefix = "[•] ";
        }

        logArea.append(prefix + message);
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Process log messages with color coding
     */
    private void processLogMessage(String msg) {
        if (msg.startsWith("SUCCESS:")) {
            logSuccess(msg.substring(8));
        } else if (msg.startsWith("WARNING:")) {
            logWarning(msg.substring(8));
        } else if (msg.startsWith("ERROR:")) {
            logError(msg.substring(6));
        } else if (msg.startsWith("INFO:")) {
            logInfo(msg.substring(5));
        } else {
            logInfo(msg);
        }
    }

    // ==================== Actions ====================

    private void openArchive() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open Archive");
        chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".myzip");
            }
            @Override
            public String getDescription() {
                return "MyZip Archives (*.myzip)";
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadArchive(chooser.getSelectedFile());
        }
    }

    private void loadArchive(File archive) {
        currentArchive = archive;
        addressBar.setText(archive.getAbsolutePath());
        tableModel.setRowCount(0);
        currentFiles.clear();

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                updateStatus("Loading archive...");
                showProgress(true);
                progressBar.setIndeterminate(true);
                publish("INFO:Opening: " + archive.getName() + "\n");

                try {
                    ArchiveManager manager = new ArchiveManager();
                    ArchiveManager.DecompressionResult result = manager.extractArchive(archive);
                    currentFiles = result.getRestoredFiles();

                    publish("INFO:Found " + currentFiles.size() + " files in archive\n");

                    long totalSize = 0;
                    for (ArchiveManager.RestoredFile file : currentFiles) {
                        totalSize += file.getData().length;
                    }

                    final long finalTotal = totalSize;
                    SwingUtilities.invokeLater(() -> {
                        for (ArchiveManager.RestoredFile file : currentFiles) {
                            String name = file.getPath();
                            String size = formatBytes(file.getData().length);
                            String compressed = "-";
                            String type = getFileType(name);
                            String modified = "-";
                            tableModel.addRow(new Object[]{name, size, compressed, type, modified});
                        }
                        sizeLabel.setText(currentFiles.size() + " files | " + formatBytes(finalTotal));
                    });

                    publish("SUCCESS:Archive loaded successfully\n");
                } catch (Exception e) {
                    publish("ERROR:Error loading archive: " + e.getMessage() + "\n");
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    processLogMessage(msg);
                }
            }

            @Override
            protected void done() {
                showProgress(false);
                updateStatus("Ready");
            }
        };

        worker.execute();
    }

    private void closeArchive() {
        currentArchive = null;
        currentFiles.clear();
        tableModel.setRowCount(0);
        addressBar.setText("No archive open - Drag & drop files to compress, or use 'Compress' button");
        sizeLabel.setText("0 files | 0 B");
        logInfo("Archive closed\n");
    }

    private void compressFiles() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setMultiSelectionEnabled(true);
        chooser.setDialogTitle("Select Files or Folder to Compress");

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = chooser.getSelectedFiles();

            // Choose save location
            JFileChooser saveChooser = new JFileChooser();
            saveChooser.setDialogTitle("Save Archive As");
            saveChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            saveChooser.setSelectedFile(new File(saveChooser.getCurrentDirectory(), "archive.myzip"));

            if (saveChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File outputFile = saveChooser.getSelectedFile();

                if (outputFile.isDirectory()) {
                    outputFile = new File(outputFile, "archive.myzip");
                } else if (!outputFile.getName().endsWith(".myzip")) {
                    outputFile = new File(outputFile.getAbsolutePath() + ".myzip");
                }

                performCompression(selectedFiles, outputFile);
            }
        }
    }

    /**
     * Perform compression with enhanced progress dialog and cancellation support
     */
    private void performCompression(File[] selectedFiles, File outputFile) {
        operationCancelled = false;

        // Create enhanced progress dialog with cancel button
        EnhancedProgressDialog progressDialog = new EnhancedProgressDialog(
            this,
            "Compressing Files",
            () -> {
                operationCancelled = true;
                if (currentWorker != null && !currentWorker.isDone()) {
                    currentWorker.cancel(true);
                }
            }
        );

        currentWorker = new SwingWorker<ArchiveManager.CompressionResult, String>() {
            @Override
            protected ArchiveManager.CompressionResult doInBackground() throws Exception {
                setButtonsEnabled(false);

                if (operationCancelled) return null;

                progressDialog.setStatus("Collecting files...");
                progressDialog.setProgress(0);
                publish("INFO:Starting compression...\n");

                List<ArchiveManager.FileEntry> fileEntries = new ArrayList<>();
                for (File file : selectedFiles) {
                    if (operationCancelled) {
                        publish("WARNING:Compression cancelled by user\n");
                        return null;
                    }
                    collectFiles(file, file.getParentFile(), fileEntries);
                }

                if (operationCancelled) return null;

                progressDialog.setStatus("Compressing " + fileEntries.size() + " files...");
                progressDialog.setProgress(30);
                publish("INFO:Processing " + fileEntries.size() + " files...\n");

                // Compress with per-file progress updates
                ArchiveManager manager = new ArchiveManager();

                // Update progress per file
                int totalFiles = fileEntries.size();

                for (int i = 0; i < fileEntries.size(); i++) {
                    if (operationCancelled) {
                        publish("WARNING:Compression cancelled by user\n");
                        return null;
                    }

                    ArchiveManager.FileEntry entry = fileEntries.get(i);
                    progressDialog.setDetail("Compressing: " + entry.getFileName());

                    int progress = 30 + (i * 60 / totalFiles);
                    progressDialog.setProgress(progress);

                    // Small delay to make progress visible (optional)
                    if (i % 5 == 0) {
                        Thread.sleep(10);
                    }
                }

                if (operationCancelled) return null;

                ArchiveManager.CompressionResult result = manager.createArchive(fileEntries, outputFile);

                progressDialog.setProgress(100);
                progressDialog.setStatus("Compression Complete!");
                progressDialog.setDetail("Archive created successfully");

                return result;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    processLogMessage(msg);
                }
            }

            @Override
            protected void done() {
                try {
                    ArchiveManager.CompressionResult result = get();

                    if (result != null && !operationCancelled) {
                        logSuccess("\n=== Compression Complete ===\n");
                        logInfo("Original Size: " + formatBytes(result.getOriginalSize()) + "\n");
                        logInfo("Compressed Size: " + formatBytes(result.getCompressedSize()) + "\n");
                        logSuccess("Compression Ratio: " + String.format("%.2f%%", result.getCompressionRatio()) + "\n");
                        logInfo("Duplicates Found: " + result.getDuplicateCount() + "\n");
                        logInfo("Archive saved to: " + outputFile.getAbsolutePath() + "\n\n");

                        // Wait a moment then load the archive
                        javax.swing.Timer timer = new javax.swing.Timer(800, e -> loadArchive(outputFile));
                        timer.setRepeats(false);
                        timer.start();
                    }
                } catch (Exception e) {
                    if (!operationCancelled) {
                        logError("Compression failed: " + e.getMessage() + "\n");
                        e.printStackTrace();
                    }
                } finally {
                    progressDialog.dispose();
                    setButtonsEnabled(true);
                    updateStatus("Ready");
                    currentWorker = null;
                }
            }
        };

        // Show dialog in separate thread
        new Thread(() -> progressDialog.setVisible(true)).start();
        currentWorker.execute();
    }

    private void extractArchive() {
        if (currentArchive == null) {
            // No archive open, let user select one
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select .myzip Archive");
            chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().toLowerCase().endsWith(".myzip");
                }
                @Override
                public String getDescription() {
                    return "MyZip Archives (*.myzip)";
                }
            });

            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            currentArchive = chooser.getSelectedFile();
        }

        JFileChooser dirChooser = new JFileChooser();
        dirChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setDialogTitle("Extract to...");
        dirChooser.setApproveButtonText("Extract Here");

        if (dirChooser.showDialog(this, "Select Folder") == JFileChooser.APPROVE_OPTION) {
            File outputDir = dirChooser.getSelectedFile();

            if (outputDir == null || !outputDir.isDirectory()) {
                JOptionPane.showMessageDialog(this,
                    "Please select a valid directory",
                    "Invalid Selection",
                    JOptionPane.ERROR_MESSAGE);
                return;
            }

            performExtraction(currentArchive, outputDir);
        }
    }

    /**
     * Perform extraction with enhanced progress and cancellation
     */
    private void performExtraction(File archiveFile, File outputDir) {
        operationCancelled = false;

        // Create enhanced progress dialog with cancel button
        EnhancedProgressDialog progressDialog = new EnhancedProgressDialog(
            this,
            "Extracting Files",
            () -> {
                operationCancelled = true;
                if (currentWorker != null && !currentWorker.isDone()) {
                    currentWorker.cancel(true);
                }
            }
        );

        currentWorker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                setButtonsEnabled(false);

                progressDialog.setStatus("Reading archive...");
                progressDialog.setProgress(0);
                publish("INFO:Starting extraction...\n");
                publish("INFO:Archive: " + archiveFile.getName() + "\n\n");

                if (operationCancelled) return null;

                ArchiveManager manager = new ArchiveManager();
                ArchiveManager.DecompressionResult result = manager.extractArchive(archiveFile);

                if (operationCancelled) {
                    publish("WARNING:Extraction cancelled by user\n");
                    return null;
                }

                progressDialog.setStatus("Extracting " + result.getRestoredFiles().size() + " files...");
                progressDialog.setProgress(10);
                publish("INFO:Extracting " + result.getRestoredFiles().size() + " files...\n");

                int verified = 0;
                int current = 0;
                int total = result.getRestoredFiles().size();

                for (ArchiveManager.RestoredFile file : result.getRestoredFiles()) {
                    if (operationCancelled) {
                        publish("WARNING:Extraction cancelled by user\n");
                        return null;
                    }

                    File outputFile = new File(outputDir, file.getPath());
                    outputFile.getParentFile().mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(file.getData());
                    }

                    current++;
                    int progress = 10 + (current * 85 / total);
                    progressDialog.setProgress(progress);
                    progressDialog.setDetail("Extracting: " + file.getPath());

                    if (file.isVerified()) {
                        verified++;
                        publish("SUCCESS:" + file.getPath() + "\n");
                    } else {
                        publish("WARNING:" + file.getPath() + " (verification failed)\n");
                    }

                    // Small delay for visibility
                    if (current % 3 == 0) {
                        Thread.sleep(10);
                    }
                }

                progressDialog.setProgress(100);
                progressDialog.setStatus("Extraction Complete!");
                progressDialog.setDetail(verified + " of " + total + " files verified");

                publish("SUCCESS:\n=== Extraction Complete ===\n");
                publish("INFO:Files extracted: " + result.getRestoredFiles().size() + "\n");
                publish("SUCCESS:Verified: " + verified + "/" + result.getRestoredFiles().size() + "\n");
                publish("INFO:Output directory: " + outputDir.getAbsolutePath() + "\n\n");

                // Wait a moment to show completion
                Thread.sleep(500);

                return null;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String msg : chunks) {
                    processLogMessage(msg);
                }
            }

            @Override
            protected void done() {
                progressDialog.dispose();
                setButtonsEnabled(true);
                updateStatus("Ready");
                currentWorker = null;
            }
        };

        // Show dialog in separate thread
        new Thread(() -> progressDialog.setVisible(true)).start();
        currentWorker.execute();
    }

    private void viewSelectedFile() {
        int row = fileTable.getSelectedRow();
        if (row < 0 || row >= currentFiles.size()) {
            JOptionPane.showMessageDialog(this, "Please select a file to view", "No Selection", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        ArchiveManager.RestoredFile file = currentFiles.get(row);
        String content;

        try {
            content = new String(file.getData(), "UTF-8");
            if (content.length() > 10000) {
                content = content.substring(0, 10000) + "\n\n... (truncated)";
            }
        } catch (Exception e) {
            content = "(Binary file - cannot display)";
        }

        JTextArea textArea = new JTextArea(content);
        textArea.setEditable(false);
        textArea.setFont(new Font("Consolas", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "View: " + file.getPath(), JOptionPane.PLAIN_MESSAGE);
    }

    private void deleteSelectedFiles() {
        JOptionPane.showMessageDialog(this,
            "Delete from archive not supported in this version.\nExtract files, modify, and create a new archive.",
            "Feature Not Available",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void showArchiveInfo() {
        if (currentArchive == null) {
            JOptionPane.showMessageDialog(this, "No archive is currently open", "No Archive", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        long totalUncompressed = 0;
        for (ArchiveManager.RestoredFile file : currentFiles) {
            totalUncompressed += file.getData().length;
        }

        String info = String.format(
            "Archive: %s\n\n" +
            "Location: %s\n" +
            "Archive Size: %s\n" +
            "Files: %d\n" +
            "Uncompressed Size: %s\n" +
            "Compression Ratio: %.2f%%",
            currentArchive.getName(),
            currentArchive.getParent(),
            formatBytes(currentArchive.length()),
            currentFiles.size(),
            formatBytes(totalUncompressed),
            totalUncompressed > 0 ? (1.0 - (double)currentArchive.length() / totalUncompressed) * 100 : 0
        );

        JOptionPane.showMessageDialog(this, info, "Archive Information", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showAbout() {
        String about =
            "MyZip - File Compression Utility\n" +
            "Version 1.0\n\n" +
            "A WinZip/WinRAR style compression tool\n" +
            "supporting LZW and RLE algorithms.\n\n" +
            "Features:\n" +
            "- File and folder compression\n" +
            "- Drag & drop support\n" +
            "- Cancelable operations\n" +
            "- Deduplication support\n" +
            "- Hash verification\n\n" +
            "DSA Project - Enhanced Version";

        JOptionPane.showMessageDialog(this, about, "About MyZip", JOptionPane.INFORMATION_MESSAGE);
    }

    // ==================== Utilities ====================

    private void collectFiles(File file, File baseDir, List<ArchiveManager.FileEntry> entries) throws IOException {
        if (file.isFile()) {
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());
            String relativePath = baseDir.toPath().relativize(file.toPath()).toString();
            entries.add(new ArchiveManager.FileEntry(relativePath, file.getName(), data));
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    collectFiles(f, baseDir, entries);
                }
            }
        }
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    private void showProgress(boolean show) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setVisible(show);
            progressBar.setIndeterminate(false);
            if (!show) progressBar.setValue(0);
        });
    }

    private void setButtonsEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            compressBtn.setEnabled(enabled);
            extractBtn.setEnabled(enabled);
            viewBtn.setEnabled(enabled);
            deleteBtn.setEnabled(enabled);
            infoBtn.setEnabled(enabled);
        });
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private String getFileType(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot > 0 && dot < filename.length() - 1) {
            String ext = filename.substring(dot + 1).toUpperCase();
            switch (ext) {
                case "TXT": return "Text File";
                case "PDF": return "PDF Document";
                case "DOC": case "DOCX": return "Word Document";
                case "XLS": case "XLSX": return "Excel Spreadsheet";
                case "JPG": case "JPEG": case "PNG": case "GIF": return "Image";
                case "MP3": case "WAV": case "FLAC": return "Audio";
                case "MP4": case "AVI": case "MKV": return "Video";
                case "JAVA": return "Java Source";
                case "PY": return "Python Script";
                case "JS": return "JavaScript";
                case "HTML": return "HTML Document";
                case "CSS": return "Stylesheet";
                case "JSON": return "JSON File";
                case "XML": return "XML File";
                case "ZIP": case "RAR": case "7Z": return "Archive";
                default: return ext + " File";
            }
        }
        return "File";
    }

    /**
     * Enhanced Progress Dialog with Cancel button support
     */
    private static class EnhancedProgressDialog extends JDialog {
        private JProgressBar progressBar;
        private JLabel statusLabel;
        private JLabel detailLabel;
        private JButton cancelButton;

        public EnhancedProgressDialog(JFrame parent, String title, Runnable onCancel) {
            super(parent, title, true);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            setSize(550, 200);
            setLocationRelativeTo(parent);
            setResizable(false);

            // Modern background
            JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
            mainPanel.setBackground(new Color(250, 252, 255));

            // Header
            JPanel headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(new Color(240, 248, 255));
            headerPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(220, 230, 245)),
                new EmptyBorder(16, 20, 16, 20)
            ));

            statusLabel = new JLabel("Initializing...");
            statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            statusLabel.setForeground(new Color(60, 70, 90));
            headerPanel.add(statusLabel, BorderLayout.CENTER);

            mainPanel.add(headerPanel, BorderLayout.NORTH);

            // Content panel
            JPanel contentPanel = new JPanel(new BorderLayout(0, 12));
            contentPanel.setBackground(new Color(250, 252, 255));
            contentPanel.setBorder(new EmptyBorder(20, 24, 20, 24));

            // Progress bar
            progressBar = new JProgressBar(0, 100);
            progressBar.setStringPainted(true);
            progressBar.setPreferredSize(new Dimension(480, 32));
            progressBar.setForeground(new Color(66, 135, 245));
            progressBar.setFont(new Font("Segoe UI", Font.BOLD, 11));
            progressBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 230, 245), 2),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
            ));
            contentPanel.add(progressBar, BorderLayout.NORTH);

            // Bottom panel with detail label and cancel button
            JPanel bottomPanel = new JPanel(new BorderLayout(12, 0));
            bottomPanel.setBackground(new Color(250, 252, 255));

            detailLabel = new JLabel(" ");
            detailLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            detailLabel.setForeground(new Color(120, 120, 120));
            bottomPanel.add(detailLabel, BorderLayout.CENTER);

            // Cancel button with modern styling
            cancelButton = new JButton("Cancel");
            cancelButton.setPreferredSize(new Dimension(100, 32));
            cancelButton.setFont(new Font("Segoe UI", Font.BOLD, 11));
            cancelButton.setBackground(new Color(240, 70, 70));
            cancelButton.setForeground(Color.WHITE);
            cancelButton.setFocusPainted(false);
            cancelButton.setBorderPainted(false);
            cancelButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

            // Hover effect
            cancelButton.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (cancelButton.isEnabled()) {
                        cancelButton.setBackground(new Color(220, 50, 50));
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    cancelButton.setBackground(new Color(240, 70, 70));
                }
            });

            cancelButton.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Are you sure you want to cancel this operation?",
                    "Confirm Cancellation",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    cancelButton.setEnabled(false);
                    cancelButton.setText("Cancelling...");
                    cancelButton.setBackground(new Color(180, 180, 180));
                    onCancel.run();
                }
            });
            bottomPanel.add(cancelButton, BorderLayout.EAST);

            contentPanel.add(bottomPanel, BorderLayout.CENTER);

            mainPanel.add(contentPanel, BorderLayout.CENTER);

            add(mainPanel);
        }

        public void setProgress(int value) {
            SwingUtilities.invokeLater(() -> {
                progressBar.setValue(value);
                if (value >= 100) {
                    cancelButton.setEnabled(false);
                    cancelButton.setBackground(new Color(180, 180, 180));
                }
            });
        }

        public void setStatus(String status) {
            SwingUtilities.invokeLater(() -> statusLabel.setText(status));
        }

        public void setDetail(String detail) {
            SwingUtilities.invokeLater(() -> detailLabel.setText(detail));
        }

        public void setIndeterminate(boolean indeterminate) {
            SwingUtilities.invokeLater(() -> progressBar.setIndeterminate(indeterminate));
        }
    }

    public static void main(String[] args) {
        // Setup FlatLaf modern look and feel
        try {
            // Use FlatLaf Light theme
            FlatLightLaf.setup();

            // Customize FlatLaf properties for a modern, professional look
            UIManager.put("Button.arc", 10);                   // Rounded buttons
            UIManager.put("Component.arc", 10);                // Rounded components
            UIManager.put("ProgressBar.arc", 10);              // Rounded progress bars
            UIManager.put("TextComponent.arc", 8);             // Rounded text fields
            UIManager.put("ScrollBar.width", 12);              // Thinner scrollbars
            UIManager.put("ScrollBar.trackArc", 999);          // Fully rounded scrollbar track
            UIManager.put("ScrollBar.thumbArc", 999);          // Fully rounded scrollbar thumb
            UIManager.put("Table.showHorizontalLines", false); // Cleaner table
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 0));

            // Modern color palette
            UIManager.put("Button.hoverBackground", new Color(230, 240, 250));
            UIManager.put("Component.focusWidth", 2);
            UIManager.put("Component.focusColor", new Color(66, 135, 245));
            UIManager.put("Component.borderColor", new Color(220, 225, 235));

            // Table colors
            UIManager.put("Table.selectionBackground", new Color(66, 135, 245));
            UIManager.put("Table.selectionForeground", Color.WHITE);

            // ProgressBar colors
            UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
            UIManager.put("ProgressBar.selectionBackground", new Color(60, 60, 60));

            // Menu colors
            UIManager.put("Menu.selectionBackground", new Color(230, 240, 250));
            UIManager.put("MenuItem.selectionBackground", new Color(230, 240, 250));

        } catch (Exception e) {
            System.err.println("Failed to initialize FlatLaf, using default look and feel");
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            CompressionGUI gui = new CompressionGUI();
            gui.setVisible(true);
        });
    }
}
