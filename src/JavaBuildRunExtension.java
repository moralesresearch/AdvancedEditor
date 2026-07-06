import javax.swing.KeyStroke;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles the current buffer as a Java source file and runs it, streaming the
 * compiler and program output into the editor's {@link OutputConsole}.
 *
 * The work runs on a background thread so the UI stays responsive, and the
 * source is written to {@code <ClassName>.java} (matching the declared class)
 * so {@code javac} accepts it.
 */
public class JavaBuildRunExtension implements TextEditorExtension {

    private static final Pattern PUBLIC_CLASS =
            Pattern.compile("public\\s+(?:final\\s+|abstract\\s+|strictfp\\s+)*class\\s+(\\w+)");
    private static final Pattern ANY_CLASS =
            Pattern.compile("\\bclass\\s+(\\w+)");

    @Override
    public String getName() {
        return "Compile and Run Java";
    }

    @Override
    public KeyStroke getAccelerator() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_R,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());
    }

    @Override
    public void execute(TextEditor editor) {
        OutputConsole console = editor.getOutputConsole();
        console.clear();
        console.show();

        String source = editor.getTextArea().getText();
        if (source.isBlank()) {
            console.appendLine("[Nothing to run — the document is empty.]");
            return;
        }

        Thread worker = new Thread(() -> compileAndRun(source, console), "java-build-run");
        worker.setDaemon(true);
        worker.start();
    }

    private void compileAndRun(String source, OutputConsole console) {
        Path dir = null;
        try {
            String className = detectClassName(source);
            dir = Files.createTempDirectory("jte-run");
            Path javaFile = dir.resolve(className + ".java");
            Files.writeString(javaFile, source);

            console.appendLine("> javac " + className + ".java");
            int compileExit = runProcess(dir.toFile(), console,
                    tool("javac"), javaFile.toString());
            if (compileExit != 0) {
                console.appendLine("");
                console.appendLine("[Compilation failed — exit code " + compileExit + "]");
                return;
            }

            console.appendLine("");
            console.appendLine("> java " + className);
            console.appendLine("");
            int runExit = runProcess(dir.toFile(), console,
                    tool("java"), "-cp", dir.toString(), className);
            console.appendLine("");
            console.appendLine("[Process finished — exit code " + runExit + "]");
        } catch (Exception ex) {
            console.appendLine("");
            console.appendLine("[Error: " + ex.getMessage() + "]");
        } finally {
            if (dir != null) {
                deleteRecursively(dir.toFile());
            }
        }
    }

    /** Runs a command, streaming merged stdout/stderr to the console line by line. */
    private int runProcess(File workingDir, OutputConsole console, String... command)
            throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                console.appendLine(line);
            }
        }
        return process.waitFor();
    }

    /** Uses the public class name if present, else any class, else "Main". */
    private String detectClassName(String source) {
        Matcher m = PUBLIC_CLASS.matcher(source);
        if (m.find()) {
            return m.group(1);
        }
        m = ANY_CLASS.matcher(source);
        if (m.find()) {
            return m.group(1);
        }
        return "Main";
    }

    /** Resolves a JDK tool from java.home so it works regardless of PATH. */
    private String tool(String name) {
        String home = System.getProperty("java.home");
        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String exe = windows ? name + ".exe" : name;
        File candidate = new File(new File(home, "bin"), exe);
        return candidate.isFile() ? candidate.getAbsolutePath() : name;
    }

    private void deleteRecursively(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }
        file.delete();
    }
}
