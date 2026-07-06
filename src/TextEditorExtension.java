import javax.swing.KeyStroke;

/**
 * A pluggable command that operates on the editor. Implementations are
 * registered with an {@link ExtensionManager} and surfaced in the Tools menu;
 * the manager and menu treat them polymorphically through this interface.
 */
public interface TextEditorExtension {

    /** Human-readable name, used as the Tools-menu label and lookup key. */
    String getName();

    /** Performs the extension's action against the given editor. */
    void execute(TextEditor editor);

    /**
     * Optional keyboard shortcut for the menu item, or {@code null} for none.
     * Provided as a default so simple extensions don't have to implement it.
     */
    default KeyStroke getAccelerator() {
        return null;
    }
}
