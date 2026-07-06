import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ButtonGroup;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * A simple plain-text editor in the spirit of the classic Windows Notepad
 * (pre-2022) and gedit, rendered with the Nimbus look and feel.
 *
 * <p>This class is the window/controller: it owns the frame, wires the menus to
 * actions, and tracks document state. Self-contained pieces of UI live in their
 * own classes — {@link StatusBar}, {@link FindReplaceDialog},
 * {@link FontChooserDialog} and {@link OutputConsole} — and pluggable commands
 * come in through the {@link ExtensionManager}.
 */
public final class TextEditor {

    /** Application name shown in dialogs and the title bar. */
    private static final String APP_NAME = "JTextEditor";

    /** Release version, shown in the About dialog. */
    private static final String APP_VERSION = "2.0";

    /** True when running on macOS (uses the native screen menu bar). */
    private static final boolean MAC =
            System.getProperty("os.name", "").toLowerCase().contains("mac");

    /** All open editor windows, so the macOS Quit handler can poll them. */
    private static final List<TextEditor> OPEN_EDITORS = new ArrayList<>();

    private final JFrame frame;
    private final RSyntaxTextArea textArea;
    private final RTextScrollPane scrollPane;
    private final StatusBar statusBar;
    private final ExtensionManager extensionManager = new ExtensionManager();

    private JCheckBoxMenuItem wordWrapItem;
    private JCheckBoxMenuItem lineNumbersItem;
    private JCheckBoxMenuItem statusBarItem;
    private JMenuItem undoItem;
    private JMenuItem redoItem;

    /** Radio items for the Syntax Highlighting menu, keyed by language. */
    private final java.util.EnumMap<SyntaxLanguage, JRadioButtonMenuItem> languageItems =
            new java.util.EnumMap<>(SyntaxLanguage.class);
    /** The language currently highlighted (PLAIN by default). */
    private SyntaxLanguage currentLanguage = SyntaxLanguage.PLAIN;

    /** File currently backing this document, or null if never saved. */
    private File currentFile;
    /** True when the buffer has unsaved changes. */
    private boolean modified;

    /** Base font size (in points) before any zooming. */
    private int baseFontSize = 13;
    /** Current zoom multiplier, 1.0 == 100%. */
    private double zoom = 1.0;

    /** Lazily created collaborators. */
    private FindReplaceDialog findReplaceDialog;
    private OutputConsole outputConsole;

    /** Shared listener re-attached whenever {@code read()} swaps the document. */
    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            onDocumentChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            onDocumentChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            onDocumentChanged();
        }
    };

    public TextEditor() {
        frame = new JFrame();

        textArea = new RSyntaxTextArea();
        // Plain-text editor: no syntax highlighting, no code folding, no
        // current-line highlight -- a clean Notepad-like canvas.
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
        textArea.setCodeFoldingEnabled(false);
        textArea.setHighlightCurrentLine(false);
        textArea.setAntiAliasingEnabled(true);
        textArea.setTabSize(4);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, baseFontSize));

        scrollPane = new RTextScrollPane(textArea, true);
        scrollPane.setLineNumbersEnabled(false); // off by default, Notepad-style

        statusBar = new StatusBar();

        extensionManager.registerExtension(new JavaBuildRunExtension());

        frame.setLayout(new BorderLayout());
        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(statusBar, BorderLayout.SOUTH);
        frame.setJMenuBar(buildMenuBar());

        installListeners();

        setCurrentFile(null);
        setModified(false);
        statusBar.update(textArea);
        updateUndoRedoState();

        frame.setSize(820, 620);
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exit();
            }

            @Override
            public void windowClosed(WindowEvent e) {
                OPEN_EDITORS.remove(TextEditor.this);
            }
        });

        OPEN_EDITORS.add(this);
        frame.setVisible(true);
        textArea.requestFocusInWindow();
    }

    // ------------------------------------------------------------------
    // Accessors used by extensions
    // ------------------------------------------------------------------

    /** Exposes the underlying text area (used by editor extensions). */
    public RSyntaxTextArea getTextArea() {
        return textArea;
    }

    /** The shared, lazily created output console for build/run extensions. */
    public OutputConsole getOutputConsole() {
        if (outputConsole == null) {
            outputConsole = new OutputConsole(frame);
        }
        return outputConsole;
    }

    // ------------------------------------------------------------------
    // Menu construction
    // ------------------------------------------------------------------

    private JMenuBar buildMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(buildFileMenu());
        menuBar.add(buildEditMenu());
        menuBar.add(buildFormatMenu());
        menuBar.add(buildViewMenu());
        JMenu tools = buildToolsMenu();
        if (tools.getItemCount() > 0) {
            menuBar.add(tools);
        }
        menuBar.add(buildHelpMenu());
        return menuBar;
    }

    /** Menu accelerator mask that respects the platform (Cmd on macOS). */
    private static int menuMask() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    private JMenuItem item(String text, int keyCode, int modifiers, Runnable action) {
        JMenuItem mi = new JMenuItem(text);
        if (keyCode != 0) {
            mi.setAccelerator(KeyStroke.getKeyStroke(keyCode, modifiers));
        }
        if (action != null) {
            mi.addActionListener(e -> action.run());
        }
        return mi;
    }

    private JMenu buildFileMenu() {
        JMenu menu = new JMenu("File");
        menu.setMnemonic(KeyEvent.VK_F);
        menu.add(item("New", KeyEvent.VK_N, menuMask(), this::newDocument));
        menu.add(item("New Window", KeyEvent.VK_N, menuMask() | InputEvent.SHIFT_DOWN_MASK,
                TextEditor::new));
        menu.add(item("Open…", KeyEvent.VK_O, menuMask(), this::open));
        menu.addSeparator();
        menu.add(item("Save", KeyEvent.VK_S, menuMask(), this::save));
        menu.add(item("Save As…", KeyEvent.VK_S, menuMask() | InputEvent.SHIFT_DOWN_MASK,
                this::saveAs));
        menu.addSeparator();
        menu.add(item("Print…", KeyEvent.VK_P, menuMask(), this::print));
        menu.addSeparator();
        menu.add(item("Exit", KeyEvent.VK_Q, menuMask(), this::exit));
        return menu;
    }

    private JMenu buildEditMenu() {
        JMenu menu = new JMenu("Edit");
        menu.setMnemonic(KeyEvent.VK_E);

        undoItem = item("Undo", KeyEvent.VK_Z, menuMask(), this::undo);
        redoItem = item("Redo", KeyEvent.VK_Y, menuMask(), this::redo);
        menu.add(undoItem);
        menu.add(redoItem);
        menu.addSeparator();

        menu.add(item("Cut", KeyEvent.VK_X, menuMask(), textArea::cut));
        menu.add(item("Copy", KeyEvent.VK_C, menuMask(), textArea::copy));
        menu.add(item("Paste", KeyEvent.VK_V, menuMask(), textArea::paste));
        menu.add(item("Delete", KeyEvent.VK_DELETE, 0, this::deleteSelection));
        menu.addSeparator();

        menu.add(item("Find…", KeyEvent.VK_F, menuMask(), () -> findReplace().showFind()));
        menu.add(item("Find Next", KeyEvent.VK_F3, 0, () -> findReplace().findNext(true)));
        menu.add(item("Find Previous", KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK,
                () -> findReplace().findNext(false)));
        menu.add(item("Replace…", KeyEvent.VK_H, menuMask(), () -> findReplace().showReplace()));
        menu.add(item("Go To Line…", KeyEvent.VK_G, menuMask(), this::goToLine));
        menu.addSeparator();

        menu.add(item("Select All", KeyEvent.VK_A, menuMask(), textArea::selectAll));
        menu.add(item("Insert Date/Time", KeyEvent.VK_F5, 0, this::insertDateTime));
        return menu;
    }

    private JMenu buildFormatMenu() {
        JMenu menu = new JMenu("Format");
        menu.setMnemonic(KeyEvent.VK_O);

        wordWrapItem = new JCheckBoxMenuItem("Word Wrap");
        wordWrapItem.addActionListener(e -> {
            boolean on = wordWrapItem.isSelected();
            textArea.setLineWrap(on);
            textArea.setWrapStyleWord(on);
        });
        menu.add(wordWrapItem);
        menu.add(item("Font…", 0, 0, this::chooseFont));
        menu.addSeparator();
        menu.add(buildSyntaxMenu());
        return menu;
    }

    /** Submenu of languages, kept in sync with auto-detection on open/save. */
    private JMenu buildSyntaxMenu() {
        JMenu menu = new JMenu("Syntax Highlighting");
        ButtonGroup group = new ButtonGroup();
        for (SyntaxLanguage lang : SyntaxLanguage.values()) {
            JRadioButtonMenuItem mi = new JRadioButtonMenuItem(lang.displayName());
            mi.setSelected(lang == currentLanguage);
            mi.addActionListener(e -> setLanguage(lang));
            group.add(mi);
            languageItems.put(lang, mi);
            menu.add(mi);
        }
        return menu;
    }

    /** Applies a language: sets the highlighting style and syncs the menu. */
    private void setLanguage(SyntaxLanguage lang) {
        currentLanguage = lang;
        textArea.setSyntaxEditingStyle(lang.style());
        // Code folding only makes sense once we're highlighting real code.
        textArea.setCodeFoldingEnabled(lang != SyntaxLanguage.PLAIN);
        JRadioButtonMenuItem mi = languageItems.get(lang);
        if (mi != null) {
            mi.setSelected(true);
        }
    }

    private JMenu buildViewMenu() {
        JMenu menu = new JMenu("View");
        menu.setMnemonic(KeyEvent.VK_V);

        lineNumbersItem = new JCheckBoxMenuItem("Line Numbers");
        lineNumbersItem.addActionListener(e ->
                scrollPane.setLineNumbersEnabled(lineNumbersItem.isSelected()));
        menu.add(lineNumbersItem);

        statusBarItem = new JCheckBoxMenuItem("Status Bar", true);
        statusBarItem.addActionListener(e -> {
            statusBar.setVisible(statusBarItem.isSelected());
            frame.revalidate();
        });
        menu.add(statusBarItem);
        menu.addSeparator();

        menu.add(item("Zoom In", KeyEvent.VK_EQUALS, menuMask(), () -> setZoom(zoom + 0.1)));
        menu.add(item("Zoom Out", KeyEvent.VK_MINUS, menuMask(), () -> setZoom(zoom - 0.1)));
        menu.add(item("Restore Default Zoom", KeyEvent.VK_0, menuMask(), () -> setZoom(1.0)));
        return menu;
    }

    /** Builds the Tools menu from the registered extensions, polymorphically. */
    private JMenu buildToolsMenu() {
        JMenu menu = new JMenu("Tools");
        menu.setMnemonic(KeyEvent.VK_T);
        for (TextEditorExtension ext : extensionManager.getExtensions()) {
            JMenuItem mi = new JMenuItem(ext.getName());
            if (ext.getAccelerator() != null) {
                mi.setAccelerator(ext.getAccelerator());
            }
            mi.addActionListener(e -> extensionManager.executeExtension(ext.getName(), this));
            menu.add(mi);
        }
        return menu;
    }

    private JMenu buildHelpMenu() {
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);
        menu.add(item("About " + APP_NAME, 0, 0, this::showAbout));
        return menu;
    }

    private void installListeners() {
        textArea.addCaretListener(e -> statusBar.update(textArea));
        textArea.getDocument().addDocumentListener(documentListener);
    }

    private void onDocumentChanged() {
        setModified(true);
        statusBar.update(textArea);
        updateUndoRedoState();
    }

    // ------------------------------------------------------------------
    // File operations
    // ------------------------------------------------------------------

    private void newDocument() {
        if (!confirmDiscardChanges()) {
            return;
        }
        textArea.setText("");
        textArea.discardAllEdits();
        setCurrentFile(null);
        setModified(false);
        updateUndoRedoState();
        setLanguage(SyntaxLanguage.PLAIN);
    }

    private void open() {
        if (!confirmDiscardChanges()) {
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            loadFile(chooser.getSelectedFile());
        }
    }

    /** Reads a file into the editor, resetting modified/undo state. */
    private void loadFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            textArea.read(reader, null);
            // read() installs a fresh document, so re-attach our listener.
            textArea.getDocument().addDocumentListener(documentListener);
            textArea.discardAllEdits();
            setCurrentFile(file);
            setModified(false);
            updateUndoRedoState();
            textArea.setCaretPosition(0);
            statusBar.update(textArea);
            setLanguage(SyntaxLanguage.forFileName(file.getName()));
        } catch (IOException ex) {
            showError("Could not open file:\n" + ex.getMessage());
        }
    }

    private boolean save() {
        if (currentFile == null) {
            return saveAs();
        }
        return writeToFile(currentFile);
    }

    private boolean saveAs() {
        JFileChooser chooser = new JFileChooser();
        chooser.addChoosableFileFilter(
                new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (currentFile != null) {
            chooser.setSelectedFile(currentFile);
        }
        if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return false;
        }
        File file = chooser.getSelectedFile();
        if (file.exists()) {
            int choice = glowingConfirm("Confirm Save As",
                    file.getName() + " already exists.\nDo you want to replace it?", false);
            if (choice != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        if (writeToFile(file)) {
            setCurrentFile(file);
            // Saving Untitled as e.g. Foo.java should turn on highlighting.
            setLanguage(SyntaxLanguage.forFileName(file.getName()));
            return true;
        }
        return false;
    }

    private boolean writeToFile(File file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            textArea.write(writer);
            setModified(false);
            return true;
        } catch (IOException ex) {
            showError("Could not save file:\n" + ex.getMessage());
            return false;
        }
    }

    private void print() {
        try {
            textArea.print();
        } catch (java.awt.print.PrinterException ex) {
            showError("Could not print:\n" + ex.getMessage());
        }
    }

    private void exit() {
        if (confirmDiscardChanges()) {
            frame.dispose();
        }
    }

    // ------------------------------------------------------------------
    // Edit operations
    // ------------------------------------------------------------------

    private void undo() {
        if (textArea.canUndo()) {
            textArea.undoLastAction();
        }
        updateUndoRedoState();
    }

    private void redo() {
        if (textArea.canRedo()) {
            textArea.redoLastAction();
        }
        updateUndoRedoState();
    }

    private void deleteSelection() {
        if (textArea.getSelectedText() != null) {
            textArea.replaceSelection("");
        }
    }

    private void insertDateTime() {
        String stamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("h:mm a M/d/yyyy"));
        textArea.replaceSelection(stamp);
    }

    private FindReplaceDialog findReplace() {
        if (findReplaceDialog == null) {
            findReplaceDialog = new FindReplaceDialog(frame, textArea, APP_NAME);
        }
        return findReplaceDialog;
    }

    private void goToLine() {
        int lineCount = textArea.getLineCount();
        String input = JOptionPane.showInputDialog(frame,
                "Line number (1 - " + lineCount + "):", "Go To Line",
                JOptionPane.PLAIN_MESSAGE);
        if (input == null) {
            return;
        }
        try {
            int line = Integer.parseInt(input.trim());
            if (line < 1 || line > lineCount) {
                showError("Line number out of range.");
                return;
            }
            textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
            textArea.requestFocusInWindow();
        } catch (NumberFormatException ex) {
            showError("Please enter a valid line number.");
        } catch (BadLocationException ex) {
            showError("Could not go to line:\n" + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Format / View
    // ------------------------------------------------------------------

    private void chooseFont() {
        Font chosen = FontChooserDialog.showDialog(frame, textArea.getFont());
        if (chosen != null) {
            baseFontSize = chosen.getSize();
            zoom = 1.0;
            textArea.setFont(chosen);
            statusBar.setZoomPercent(100);
        }
    }

    private void setZoom(double newZoom) {
        zoom = Math.max(0.5, Math.min(4.0, newZoom));
        int size = Math.max(6, (int) Math.round(baseFontSize * zoom));
        textArea.setFont(textArea.getFont().deriveFont((float) size));
        statusBar.setZoomPercent((int) Math.round(zoom * 100));
    }

    private void showAbout() {
        showAboutDialog(frame);
    }

    /** Shared About dialog, also used by the macOS application menu. */
    private static void showAboutDialog(Component parent) {
        String message = """
                %s
                Version %s
                Released June 30, 2026
                Copyright (C) 2024 - 2026 Morales Research Technology Inc
                Copyright (C) 1992 - 2008 Sun Microsystems Inc.
                """.formatted(APP_NAME, APP_VERSION);
        JOptionPane.showMessageDialog(parent, message,
                "About " + APP_NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    // ------------------------------------------------------------------
    // State helpers
    // ------------------------------------------------------------------

    private void setCurrentFile(File file) {
        this.currentFile = file;
        updateTitle();
    }

    private void setModified(boolean value) {
        if (this.modified != value) {
            this.modified = value;
            updateTitle();
        }
    }

    private void updateTitle() {
        String name = (currentFile == null) ? "Untitled" : currentFile.getName();
        frame.setTitle((modified ? "*" : "") + name + " - " + APP_NAME);
    }

    private void updateUndoRedoState() {
        if (undoItem != null) {
            undoItem.setEnabled(textArea.canUndo());
        }
        if (redoItem != null) {
            redoItem.setEnabled(textArea.canRedo());
        }
    }

    /**
     * If there are unsaved changes, prompts Save / Don't Save / Cancel.
     *
     * @return true if the caller may proceed (saved or discarded),
     *         false if the user cancelled.
     */
    private boolean confirmDiscardChanges() {
        if (!modified) {
            return true;
        }
        String name = (currentFile == null) ? "Untitled" : currentFile.getName();
        int choice = glowingConfirm(APP_NAME,
                "Do you want to save changes to " + name + "?", true);
        return switch (choice) {
            case JOptionPane.YES_OPTION -> save();
            case JOptionPane.NO_OPTION -> true;
            default -> false;
        };
    }

    /**
     * A warning confirmation whose affirmative "Yes" button glows to draw the
     * eye (see {@link GlowButton}). Keeps the standard JOptionPane layout and
     * warning icon but swaps in the animated button.
     *
     * @param withCancel when true, offers Yes / No / Cancel; otherwise Yes / No
     * @return one of {@link JOptionPane}'s YES_OPTION / NO_OPTION / CANCEL_OPTION
     */
    private int glowingConfirm(String title, String message, boolean withCancel) {
        GlowButton yes = new GlowButton("Yes");
        JButton no = new JButton("No");
        JButton cancel = withCancel ? new JButton("Cancel") : null;

        Object[] options = withCancel
                ? new Object[] {cancel, no, yes}
                : new Object[] {no, yes};
        int optionType = withCancel
                ? JOptionPane.YES_NO_CANCEL_OPTION
                : JOptionPane.YES_NO_OPTION;

        JOptionPane pane = new JOptionPane(message, JOptionPane.WARNING_MESSAGE,
                optionType, null, options, yes);
        JDialog dialog = pane.createDialog(frame, title);

        yes.addActionListener(e -> {
            pane.setValue(JOptionPane.YES_OPTION);
            dialog.dispose();
        });
        no.addActionListener(e -> {
            pane.setValue(JOptionPane.NO_OPTION);
            dialog.dispose();
        });
        if (cancel != null) {
            cancel.addActionListener(e -> {
                pane.setValue(JOptionPane.CANCEL_OPTION);
                dialog.dispose();
            });
        }
        dialog.getRootPane().setDefaultButton(yes);
        dialog.setVisible(true);
        dialog.dispose();

        Object value = pane.getValue();
        return (value instanceof Integer) ? (Integer) value : JOptionPane.CANCEL_OPTION;
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, APP_NAME, JOptionPane.ERROR_MESSAGE);
    }

    // ------------------------------------------------------------------
    // Entry point and platform integration
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        if (MAC) {
            // Route the menu bar to the native macOS screen menu bar and name
            // the application menu. Must be set before AWT/Swing initializes.
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            System.setProperty("apple.awt.application.name", APP_NAME);
        }

        SwingUtilities.invokeLater(() -> {
            setNimbusLookAndFeel();
            if (MAC) {
                installMacApplicationMenu();
            }
            TextEditor editor = new TextEditor();
            // Open a file passed on the command line, if any.
            if (args.length > 0) {
                File file = new File(args[0]);
                if (file.isFile()) {
                    editor.loadFile(file);
                }
            }
        });
    }

    /** Installs the Nimbus look and feel, falling back to the system default. */
    private static void setNimbusLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // Fall back to the cross-platform default; not fatal.
            System.err.println("Could not set Nimbus look and feel: " + ex.getMessage());
        }
    }

    /**
     * Wires the native macOS application-menu items (About / Quit) to our own
     * handlers. On other platforms these live in the in-window File/Help menus.
     */
    private static void installMacApplicationMenu() {
        try {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(e -> showAboutDialog(focusedFrame()));
            }
            if (desktop.isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                desktop.setQuitHandler((e, response) -> {
                    if (quitAllEditors()) {
                        response.performQuit();
                    } else {
                        response.cancelQuit();
                    }
                });
            }
        } catch (Throwable t) {
            // Desktop integration is best-effort; ignore if unavailable.
            System.err.println("macOS application menu unavailable: " + t.getMessage());
        }
    }

    /** The focused editor frame, or any open one, for dialog parenting. */
    private static Frame focusedFrame() {
        for (TextEditor ed : OPEN_EDITORS) {
            if (ed.frame.isFocused()) {
                return ed.frame;
            }
        }
        return OPEN_EDITORS.isEmpty() ? null : OPEN_EDITORS.get(0).frame;
    }

    /**
     * Prompts each open editor to save unsaved work.
     *
     * @return true if every window is clear to close, false if the user
     *         cancelled at any window (Quit should be aborted).
     */
    private static boolean quitAllEditors() {
        for (TextEditor ed : new ArrayList<>(OPEN_EDITORS)) {
            if (!ed.confirmDiscardChanges()) {
                return false;
            }
        }
        return true;
    }
}
