import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Registry of {@link TextEditorExtension}s. The editor builds its Tools menu
 * from {@link #getExtensions()} and dispatches menu clicks back through
 * {@link #executeExtension}, so it never needs to know the concrete extension
 * types — they are handled polymorphically through the interface.
 */
public class ExtensionManager {

    private final List<TextEditorExtension> extensions = new ArrayList<>();

    /** Adds an extension to the registry. */
    public void registerExtension(TextEditorExtension extension) {
        extensions.add(extension);
    }

    /** An unmodifiable view of the registered extensions, in registration order. */
    public List<TextEditorExtension> getExtensions() {
        return Collections.unmodifiableList(extensions);
    }

    /** Runs the extension with the given name against the editor, if present. */
    public void executeExtension(String name, TextEditor editor) {
        for (TextEditorExtension ext : extensions) {
            if (ext.getName().equals(name)) {
                ext.execute(editor);
                return;
            }
        }
    }
}
