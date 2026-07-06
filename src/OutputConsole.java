import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

/**
 * A modeless console window that build/run extensions stream their output to.
 *
 * All mutating methods are safe to call from any thread: an extension can run a
 * compiler on a background thread and simply call {@link #appendLine} without
 * worrying about the Swing threading rules, because this class marshals every
 * update onto the event dispatch thread itself.
 */
public final class OutputConsole {

    private final JDialog dialog;
    private final JTextArea area;

    public OutputConsole(Frame owner) {
        dialog = new JDialog(owner, "Output", false);

        area = new JTextArea(18, 72);
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> clear());
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dialog.setVisible(false));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttons.add(clearButton);
        buttons.add(closeButton);

        dialog.setLayout(new BorderLayout());
        dialog.add(new JScrollPane(area), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
    }

    /** Makes the console visible and brings it to the front. */
    public void show() {
        onEdt(() -> {
            dialog.setVisible(true);
            dialog.toFront();
        });
    }

    /** Removes all text from the console. */
    public void clear() {
        onEdt(() -> area.setText(""));
    }

    /** Appends the given text followed by a newline. */
    public void appendLine(String text) {
        append(text + System.lineSeparator());
    }

    /** Appends the given text and scrolls to the end. */
    public void append(String text) {
        onEdt(() -> {
            area.append(text);
            area.setCaretPosition(area.getDocument().getLength());
        });
    }

    private static void onEdt(Runnable task) {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            SwingUtilities.invokeLater(task);
        }
    }
}
