import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

/**
 * The set of languages the editor can highlight, each pairing a display name
 * with its RSyntaxTextArea style constant and the file extensions that imply
 * it. Keeping this mapping in one enum lets the editor both auto-detect a
 * language from a file name and build the "Syntax Highlighting" menu from the
 * same source of truth.
 */
public enum SyntaxLanguage {

    PLAIN("Plain Text", SyntaxConstants.SYNTAX_STYLE_NONE),
    JAVA("Java", SyntaxConstants.SYNTAX_STYLE_JAVA, "java");

    // Additional languages are supported by the library and will be enabled in
    // a later release. Uncomment an entry (and add it back before the ';' above)
    // to turn it on.
    //
    // PYTHON("Python", SyntaxConstants.SYNTAX_STYLE_PYTHON, "py", "pyw"),
    // JAVASCRIPT("JavaScript", SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, "js", "mjs"),
    // TYPESCRIPT("TypeScript", SyntaxConstants.SYNTAX_STYLE_TYPESCRIPT, "ts"),
    // C("C", SyntaxConstants.SYNTAX_STYLE_C, "c", "h"),
    // CPP("C++", SyntaxConstants.SYNTAX_STYLE_CPLUSPLUS, "cpp", "cc", "cxx", "hpp"),
    // CSHARP("C#", SyntaxConstants.SYNTAX_STYLE_CSHARP, "cs"),
    // HTML("HTML", SyntaxConstants.SYNTAX_STYLE_HTML, "html", "htm"),
    // XML("XML", SyntaxConstants.SYNTAX_STYLE_XML, "xml"),
    // CSS("CSS", SyntaxConstants.SYNTAX_STYLE_CSS, "css"),
    // JSON("JSON", SyntaxConstants.SYNTAX_STYLE_JSON, "json"),
    // YAML("YAML", SyntaxConstants.SYNTAX_STYLE_YAML, "yaml", "yml"),
    // MARKDOWN("Markdown", SyntaxConstants.SYNTAX_STYLE_MARKDOWN, "md", "markdown"),
    // SQL("SQL", SyntaxConstants.SYNTAX_STYLE_SQL, "sql"),
    // SHELL("Shell", SyntaxConstants.SYNTAX_STYLE_UNIX_SHELL, "sh", "bash"),
    // PROPERTIES("Properties", SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE, "properties");

    private final String displayName;
    private final String style;
    private final String[] extensions;

    SyntaxLanguage(String displayName, String style, String... extensions) {
        this.displayName = displayName;
        this.style = style;
        this.extensions = extensions;
    }

    /** Label shown in the menu. */
    public String displayName() {
        return displayName;
    }

    /** The RSyntaxTextArea {@code SYNTAX_STYLE_*} constant. */
    public String style() {
        return style;
    }

    /** Picks the language for a file name by its extension, or PLAIN if unknown. */
    public static SyntaxLanguage forFileName(String fileName) {
        String ext = extensionOf(fileName);
        if (!ext.isEmpty()) {
            for (SyntaxLanguage lang : values()) {
                for (String candidate : lang.extensions) {
                    if (candidate.equals(ext)) {
                        return lang;
                    }
                }
            }
        }
        return PLAIN;
    }

    private static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1).toLowerCase();
    }
}
