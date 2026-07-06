import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.UIManager;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A {@link JButton} that pulses with a soft glowing halo to draw the user's
 * eye — used for the affirmative choice in warning dialogs (e.g. the "Yes"
 * button when prompting to save changes).
 *
 * <p>The animation is driven by a Swing {@link Timer} that is started when the
 * button is added to a window and stopped when it is removed, so no timer is
 * left running after its dialog closes.
 */
public final class GlowButton extends JButton {

    private static final long serialVersionUID = 1L;

    /** How much of the button's inset the halo occupies. */
    private static final int ARC = 14;

    private final Timer timer;
    private final Color glowColor;
    private double phase;

    public GlowButton(String text) {
        // Use Nimbus's own focus/selection colour so the glow matches the L&F.
        this(text, nimbusGlowColor());
    }

    public GlowButton(String text, Color glowColor) {
        super(text);
        this.glowColor = glowColor;
        setContentAreaFilled(true);
        // ~25 fps; advance the phase each tick and repaint.
        timer = new Timer(40, e -> {
            phase += 0.15;
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Oscillate 0..1 with a sine wave for a smooth breathing glow.
        float t = (float) ((Math.sin(phase) + 1.0) / 2.0);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        // A faint pulsing fill tint that keeps the label readable.
        g2.setColor(withAlpha(glowColor, 0.10f + 0.18f * t));
        g2.fillRoundRect(2, 2, w - 4, h - 4, ARC, ARC);

        // Concentric glowing rings just inside the edge form the halo.
        for (int i = 0; i < 3; i++) {
            float alpha = Math.max(0f, (0.45f + 0.45f * t) - i * 0.18f);
            g2.setColor(withAlpha(glowColor, alpha));
            g2.setStroke(new BasicStroke(1.6f + i * 1.6f));
            g2.drawRoundRect(1 + i, 1 + i, w - 3 - 2 * i, h - 3 - 2 * i, ARC, ARC);
        }
        g2.dispose();
    }

    /**
     * Resolves a glow colour from the active look and feel. Prefers Nimbus's
     * own focus/selection colours (the same ones it uses for its focus ring)
     * so the glow matches the theme, and falls back gracefully if a non-Nimbus
     * L&F is in effect.
     */
    private static Color nimbusGlowColor() {
        String[] keys = {
            "nimbusFocus",              // the colour Nimbus glows focused controls with
            "nimbusSelectionBackground",
            "nimbusSelection",
            "nimbusBase"
        };
        for (String key : keys) {
            Color c = UIManager.getColor(key);
            if (c != null) {
                // Return a plain opaque copy; alpha is applied per-frame.
                return new Color(c.getRed(), c.getGreen(), c.getBlue());
            }
        }
        // Last-resort fallback if none of the Nimbus keys are present.
        Color accent = UIManager.getColor("textHighlight");
        return (accent != null) ? accent : new Color(0x57, 0x7B, 0xB8);
    }

    private static Color withAlpha(Color base, float alpha) {
        float a = Math.max(0f, Math.min(1f, alpha));
        return new Color(base.getRed() / 255f, base.getGreen() / 255f,
                base.getBlue() / 255f, a);
    }
}
