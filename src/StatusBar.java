import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * The editor's bottom status bar. Encapsulates the caret-position, document
 * length, and zoom-level indicators so the main window only has to hand it a
 * text area and let it read out what it needs.
 */
public final class StatusBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel position = new JLabel("Ln 1, Col 1");
    private final JLabel length = new JLabel("0 chars");
    private final JLabel zoom = new JLabel("100%");

    public StatusBar() {
        super(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        right.setOpaque(false);
        right.add(length);
        right.add(zoom);

        add(position, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    /** Refreshes the caret position and character count from the text area. */
    public void update(RSyntaxTextArea textArea) {
        try {
            int caret = textArea.getCaretPosition();
            int line = textArea.getLineOfOffset(caret);
            int col = caret - textArea.getLineStartOffset(line);
            position.setText("Ln " + (line + 1) + ", Col " + (col + 1));
        } catch (BadLocationException ex) {
            position.setText("Ln 1, Col 1");
        }
        length.setText(textArea.getDocument().getLength() + " chars");
    }

    /** Updates the zoom indicator (e.g. 100, 120). */
    public void setZoomPercent(int percent) {
        zoom.setText(percent + "%");
    }
}
