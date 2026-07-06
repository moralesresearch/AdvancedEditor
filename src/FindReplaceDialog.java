import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;
import org.fife.ui.rtextarea.SearchResult;

/**
 * A modeless Find / Replace dialog. It owns the {@link SearchContext} so that
 * "Find Next" keeps working from the menu (or F3) after the dialog is closed,
 * keeping all search behaviour in one place instead of the main window.
 */
public final class FindReplaceDialog {

    private final Frame owner;
    private final RSyntaxTextArea textArea;
    private final String appName;
    private final SearchContext context = new SearchContext();

    private final JDialog dialog;
    private final JTextField findField = new JTextField(24);
    private final JTextField replaceField = new JTextField(24);
    private final JCheckBox matchCase = new JCheckBox("Match case");
    private final JCheckBox wholeWord = new JCheckBox("Whole word");
    private final JCheckBox regex = new JCheckBox("Regex");
    private final JLabel replaceLabel = new JLabel("Replace with:");

    public FindReplaceDialog(Frame owner, RSyntaxTextArea textArea, String appName) {
        this.owner = owner;
        this.textArea = textArea;
        this.appName = appName;

        dialog = new JDialog(owner, "Find and Replace", false);
        dialog.setLayout(new BorderLayout(8, 8));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        c.gridy = 0;
        fields.add(new JLabel("Find what:"), c);
        c.gridx = 1;
        fields.add(findField, c);

        c.gridx = 0;
        c.gridy = 1;
        fields.add(replaceLabel, c);
        c.gridx = 1;
        fields.add(replaceField, c);

        JPanel options = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 12, 0));
        options.add(matchCase);
        options.add(wholeWord);
        options.add(regex);
        c.gridx = 1;
        c.gridy = 2;
        fields.add(options, c);

        JButton findNextButton = new JButton("Find Next");
        JButton findPrevButton = new JButton("Find Previous");
        JButton replaceButton = new JButton("Replace");
        JButton replaceAllButton = new JButton("Replace All");
        JButton closeButton = new JButton("Close");

        findNextButton.addActionListener(e -> runFind(true));
        findPrevButton.addActionListener(e -> runFind(false));
        replaceButton.addActionListener(e -> runReplace());
        replaceAllButton.addActionListener(e -> runReplaceAll());
        closeButton.addActionListener(e -> dialog.setVisible(false));

        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 6, 6));
        buttons.add(findPrevButton);
        buttons.add(findNextButton);
        buttons.add(replaceButton);
        buttons.add(replaceAllButton);
        buttons.add(closeButton);

        dialog.add(fields, BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(findNextButton);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
    }

    /** Opens the dialog in Find-only mode. */
    public void showFind() {
        show(false);
    }

    /** Opens the dialog with the replace row visible. */
    public void showReplace() {
        show(true);
    }

    private void show(boolean replaceMode) {
        replaceLabel.setVisible(replaceMode);
        replaceField.setVisible(replaceMode);
        dialog.setTitle(replaceMode ? "Find and Replace" : "Find");

        String selected = textArea.getSelectedText();
        if (selected != null && !selected.contains("\n")) {
            findField.setText(selected);
        }
        dialog.pack();
        dialog.setVisible(true);
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    /**
     * Repeats the last search in the given direction. Used both by the dialog
     * buttons and by the editor's Find Next / Find Previous menu items; if no
     * search term has been entered yet, it simply opens the dialog.
     */
    public boolean findNext(boolean forward) {
        if (isBlank(context.getSearchFor())) {
            showFind();
            return false;
        }
        context.setSearchForward(forward);
        SearchResult result = SearchEngine.find(textArea, context);
        if (!result.wasFound()) {
            notFound(owner);
        }
        return result.wasFound();
    }

    private void syncContext() {
        context.setSearchFor(findField.getText());
        context.setReplaceWith(replaceField.getText());
        context.setMatchCase(matchCase.isSelected());
        context.setWholeWord(wholeWord.isSelected());
        context.setRegularExpression(regex.isSelected());
    }

    private void runFind(boolean forward) {
        syncContext();
        if (isBlank(context.getSearchFor())) {
            return;
        }
        findNext(forward);
    }

    private void runReplace() {
        syncContext();
        if (isBlank(context.getSearchFor())) {
            return;
        }
        context.setSearchForward(true);
        SearchResult result = SearchEngine.replace(textArea, context);
        if (!result.wasFound()) {
            notFound(dialog);
        }
    }

    private void runReplaceAll() {
        syncContext();
        if (isBlank(context.getSearchFor())) {
            return;
        }
        SearchResult result = SearchEngine.replaceAll(textArea, context);
        JOptionPane.showMessageDialog(dialog,
                result.getCount() + " replacement(s) made.",
                appName, JOptionPane.INFORMATION_MESSAGE);
    }

    private void notFound(java.awt.Component parent) {
        JOptionPane.showMessageDialog(parent,
                "Cannot find \"" + context.getSearchFor() + "\"",
                appName, JOptionPane.INFORMATION_MESSAGE);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }
}
