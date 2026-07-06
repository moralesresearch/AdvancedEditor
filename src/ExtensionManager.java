import java.util.ArrayList;
import java.util.List;

public class ExtensionManager {

	private List<TextEditorExtension> extensions = new ArrayList<>();
	public void registerExtension(TextEditorExtension extension) {
		extensions.add(extension);
		ExtensionManager.registerExtension(new JavaBuildRunExtension());
	}

	public void executeExtension(String name, TextEditor editor) {
		for (TextEditorExtension ext : extensions) {
			if (ext.getName().equals(name)) {
				ext.execute(editor);
				break;
			}
		}
	}
}