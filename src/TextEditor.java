import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Desktop;
import java.awt.desktop.AboutEvent;
import java.awt.desktop.AboutHandler;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

public class TextEditor {
    private JFrame frame;
    private RSyntaxTextArea textArea;
    private RTextScrollPane sp;
    private JTabbedPane tabbedPane;
    private JMenuBar menuBar;
    private JMenu fileMenu, formatMenu;
    private static JMenu helpMenu;
    private JMenuItem newItem, openItem, saveItem, exitItem;
    private static JMenuItem aboutItem;
    private JComboBox<String> fontBox, fontSizeBox, languageBox;
    private JCheckBox boldCheck, italicCheck;
    private JToggleButton modeToggle;
    
    ExtensionManager extensionManager = new ExtensionManager();

    public TextEditor() {
        frame = new JFrame("JText Editor");
        textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setCodeFoldingEnabled(true);
        sp = new RTextScrollPane(textArea);
        
        tabbedPane = new JTabbedPane();
        addTab();
        
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        JToolBar toolBar = new JToolBar();

        //formatMenu = new JMenu("Format");
        helpMenu = new JMenu("Help");
        newItem = new JMenuItem("New");
        openItem = new JMenuItem("Open");
        saveItem = new JMenuItem("Save");
        exitItem = new JMenuItem("Exit");
        JMenuItem newWindowItem = new JMenuItem("New Window");
        newWindowItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new TextEditor();
            }
        });
        JMenuItem newTabItem = new JMenuItem("New Tab");
        newTabItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addTab();
            }
        });

        JMenuItem closeTabItem = new JMenuItem("Close Tab");
        closeTabItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                if (tabbedPane.getTabCount() > 1) {
                    tabbedPane.remove(selectedIndex);
                } else {
                    frame.dispose();
                }
            }
        });

        JMenuItem closeWindowItem = new JMenuItem("Close Window");
        closeWindowItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                frame.dispose();
            }
        });

        fileMenu.add(closeTabItem);
        fileMenu.add(closeWindowItem);
        fileMenu.insert(newWindowItem, 0);
        fileMenu.insert(newTabItem, 1);
        fileMenu.insertSeparator(2);

        frame.add(tabbedPane, BorderLayout.CENTER);
        aboutItem = new JMenuItem("About");

        modeToggle = new JToggleButton("Switch to Programmer Mode");
        modeToggle.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (modeToggle.isSelected()) {
                    modeToggle.setText("Switch to Text Mode");
                    textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                } else {
                    modeToggle.setText("Switch to Programmer Mode");
                    updateFont();
                }
            }
        });

        String[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        fontBox = new JComboBox<>(fonts);
        fontBox.setSelectedItem("SansSerif");
        fontBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });
        String[] sizes = {"8", "10", "12", "14", "16", "18", "20", "24", "28", "32", "36", "40", "48", "56", "72"};
        fontSizeBox = new JComboBox<>(sizes);
        fontSizeBox.setSelectedItem("12");
        fontSizeBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });

        boldCheck = new JCheckBox("Bold");
        italicCheck = new JCheckBox("Italic");
        boldCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });
        italicCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateFont();
            }
        });

        String[] languages = {"None", "Java", "Python", "JavaScript", "C++", "HTML", "XML", "SQL"};
        languageBox = new JComboBox<>(languages);
        languageBox.setEditable(false);
        languageBox.setSelectedItem("None");
        languageBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String selectedLanguage = (String) languageBox.getSelectedItem();
                switch (selectedLanguage) {
                    case "Java":
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
                        break;
                    case "Python":
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
                        break;
                    case "JavaScript":
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                        break;
                    case "C++":
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS);
                        break;
                    case "HTML":
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                        break;
                    case "XML":
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                        break;
                    case "SQL":
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_SQL);
                        break;
                    default:
                        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                        break;
                }
            }
        });

        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        textArea.read(br, null);
                        br.close();

                        // Auto-detect language based on file extension
                        if (modeToggle.isSelected()) {
                            String extension = getFileExtension(file);
                            switch (extension) {
                                case "java":
                                    languageBox.setSelectedItem("Java");
                                    break;
                                case "py":
                                    languageBox.setSelectedItem("Python");
                                    break;
                                // ... Add other cases for different extensions
                                default:
                                    languageBox.setSelectedItem("None");
                                    break;
                            }
                        }

                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        newItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                textArea.setText("");
            }
        });

        openItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showOpenDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(file));
                        textArea.read(br, null);
                        br.close();
                        frame.setTitle("JText Editor - " + file.getName());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        saveItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                int option = fileChooser.showSaveDialog(frame);
                if (option == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
                        textArea.write(bw);
                        bw.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        closeTabItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, ActionEvent.CTRL_MASK));
        closeTabItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = tabbedPane.getSelectedIndex();
                RSyntaxTextArea currentTextArea = (RSyntaxTextArea) ((RTextScrollPane) tabbedPane
                        .getComponentAt(selectedIndex)).getViewport().getView();
                if (!currentTextArea.getText().isEmpty()) {
                    int option = JOptionPane.showConfirmDialog(frame,
                            "Do you want to close this tab? Unsaved changes will be lost.", "Close Tab",
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (option == JOptionPane.YES_OPTION) {
                        closeCurrentTabOrWindow(selectedIndex);
                    }
                } else {
                    closeCurrentTabOrWindow(selectedIndex);
                }
            }
        });

        closeWindowItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, ActionEvent.CTRL_MASK));
        closeWindowItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int option = JOptionPane.showConfirmDialog(frame,
                        "Do you want to close this window? Unsaved changes will be lost.", "Close Window",
                        JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (option == JOptionPane.YES_OPTION) {
                    frame.dispose();
                }
            }
        });

        exitItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        aboutItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(frame, "JText Editor Release I", "About", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        
        toolBar.add(fontBox);
        toolBar.add(fontSizeBox);
        toolBar.add(boldCheck);
        toolBar.add(italicCheck);
        toolBar.add(languageBox);
        // Add the toolbar to the frame
        frame.add(toolBar, BorderLayout.NORTH);

        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);

        frame.setJMenuBar(menuBar);
        frame.add(sp, BorderLayout.CENTER);
        frame.add(modeToggle, BorderLayout.SOUTH);
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        if (System.getProperty("os.name").contains("Mac")) {
            Desktop desktop = Desktop.getDesktop();
            desktop.setAboutHandler(new AboutHandler() {
                @Override
                public void handleAbout(AboutEvent e) {

                    JOptionPane.showMessageDialog(frame, "JText Editor Release I\n Developed by Abdon Morales", "About", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        }
    }
    
    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // No extension
        }
        return name.substring(lastIndexOf + 1);
    }

    private void updateFont() {
        String selectedFont = (String) fontBox.getSelectedItem();
        int selectedSize = Integer.parseInt((String) fontSizeBox.getSelectedItem());
        int style = Font.PLAIN;
        if (boldCheck.isSelected()) style |= Font.BOLD;
        if (italicCheck.isSelected()) style |= Font.ITALIC;
        textArea.setFont(new Font(selectedFont, style, selectedSize));
    }
    
    private void addTab() {
        RSyntaxTextArea newTextArea = new RSyntaxTextArea(20, 60);
        newTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        newTextArea.setCodeFoldingEnabled(true);
        RTextScrollPane newScrollPane = new RTextScrollPane(newTextArea);
        tabbedPane.addTab("Untitled", newScrollPane);
        int index = tabbedPane.indexOfComponent(newScrollPane);

        // Create a panel with a close button
        JPanel pnlTab = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlTab.setOpaque(false);
        JLabel lblTitle = new JLabel("Untitled");
        JButton btnClose = new JButton("x");
        btnClose.setFont(new Font("Arial", Font.BOLD, 12));
        btnClose.setMargin(new Insets(0, 0, 0, 0));
        btnClose.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = tabbedPane.indexOfComponent(newScrollPane);
                closeCurrentTabOrWindow(selectedIndex);
            }
        });
        pnlTab.add(lblTitle);
        pnlTab.add(btnClose);
        tabbedPane.setTabComponentAt(index, pnlTab);

        tabbedPane.setSelectedComponent(newScrollPane);

    }
    private void closeCurrentTabOrWindow(int selectedIndex) {
            if (tabbedPane.getTabCount() > 1) {
                tabbedPane.remove(selectedIndex);
            } else {
                frame.dispose();
            }
        }
    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "JText Editor Release I");
        if (!System.getProperty("os.name").contains("Mac")) {
            helpMenu.add(aboutItem);
        }
        new TextEditor();
    }
}
