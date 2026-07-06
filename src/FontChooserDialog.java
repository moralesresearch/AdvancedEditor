import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;

/**
 * A minimal modal font picker in the style of Notepad's Format &gt; Font
 * dialog (Swing has no built-in one). Kept as a small self-contained utility:
 * call {@link #showDialog} and get back the chosen {@link Font}, or {@code null}
 * if the user cancelled.
 */
public final class FontChooserDialog {

    private FontChooserDialog() {
        // Utility class; not instantiable.
    }

    public static Font showDialog(Frame owner, Font current) {
        final Font[] result = {null};
        JDialog dialog = new JDialog(owner, "Font", true);
        dialog.setLayout(new BorderLayout(8, 8));

        String[] families = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        JList<String> familyList = new JList<>(families);
        familyList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        familyList.setSelectedValue(current.getFamily(), true);

        String[] styles = {"Regular", "Bold", "Italic", "Bold Italic"};
        JList<String> styleList = new JList<>(styles);
        styleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        styleList.setSelectedIndex(current.getStyle());

        Integer[] sizes = {8, 9, 10, 11, 12, 13, 14, 16, 18, 20, 24, 28, 32, 36, 48, 72};
        JList<Integer> sizeList = new JList<>(sizes);
        sizeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sizeList.setSelectedValue(current.getSize(), true);
        if (sizeList.getSelectedIndex() < 0) {
            sizeList.setSelectedValue(12, true);
        }

        JLabel preview = new JLabel("AaBbYyZz 123", SwingConstants.CENTER);
        preview.setBorder(BorderFactory.createTitledBorder("Preview"));
        preview.setPreferredSize(new Dimension(120, 60));

        Runnable updatePreview = () -> {
            String fam = familyList.getSelectedValue();
            if (fam == null) {
                return;
            }
            int style = Math.max(0, styleList.getSelectedIndex());
            Integer size = sizeList.getSelectedValue();
            if (size == null) {
                size = 12;
            }
            preview.setFont(new Font(fam, style, size));
        };
        familyList.addListSelectionListener(e -> updatePreview.run());
        styleList.addListSelectionListener(e -> updatePreview.run());
        sizeList.addListSelectionListener(e -> updatePreview.run());
        updatePreview.run();

        JPanel lists = new JPanel(new GridLayout(1, 3, 8, 8));
        lists.setBorder(BorderFactory.createEmptyBorder(10, 10, 4, 10));
        lists.add(labeled("Font", new JScrollPane(familyList)));
        lists.add(labeled("Style", new JScrollPane(styleList)));
        lists.add(labeled("Size", new JScrollPane(sizeList)));

        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancel");
        ok.addActionListener(e -> {
            String fam = familyList.getSelectedValue();
            int style = Math.max(0, styleList.getSelectedIndex());
            Integer size = sizeList.getSelectedValue();
            if (fam != null && size != null) {
                result[0] = new Font(fam, style, size);
            }
            dialog.dispose();
        });
        cancel.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        buttons.add(ok);
        buttons.add(cancel);

        JPanel south = new JPanel(new BorderLayout());
        south.add(preview, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.SOUTH);

        dialog.add(lists, BorderLayout.CENTER);
        dialog.add(south, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(ok);
        dialog.setSize(520, 380);
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
        return result[0];
    }

    private static JPanel labeled(String title, Component comp) {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.add(new JLabel(title), BorderLayout.NORTH);
        panel.add(comp, BorderLayout.CENTER);
        return panel;
    }
}
