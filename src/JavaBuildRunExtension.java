import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class JavaBuildRunExtension implements TextEditorExtension {
	@Override
	public String getName() {
		return "Java Build and Run";
	}

	@Override
	public void execute(TextEditor editor) {
        String code = editor.getTextArea().getText();
        // Save to a temporary file
        File tempFile;
        try {
            tempFile = File.createTempFile("TempJavaFile", ".java");
            try (FileWriter writer = new FileWriter(tempFile)) {
                writer.write(code);
            }

            // Compile
            Process compileProcess = new ProcessBuilder("javac", tempFile.getAbsolutePath()).start();
            compileProcess.waitFor();

            if (compileProcess.exitValue() == 0) {
                // Run
                String className = tempFile.getName().replace(".java", "");
                Process runProcess = new ProcessBuilder("java", "-cp", tempFile.getParent(), className).start();
                
                // Capture output and errors
                BufferedReader stdInput = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                BufferedReader stdError = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));

                // Read the output from the command
                String s = null;
                while ((s = stdInput.readLine()) != null) {
                    System.out.println(s);
                }

                // Read any errors from the attempted command
                while ((s = stdError.readLine()) != null) {
                    System.err.println(s);
                }

                runProcess.waitFor();
            } else {
                System.err.println("Compilation failed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
	}
    }
}
