import tkinter as tk
from tkinter import filedialog, messagebox, scrolledtext, Menu, simpledialog
from docx import Document
from docx.shared import Pt
from docx.enum.text import WD_PARAGRAPH_ALIGNMENT
from docx.oxml.ns import qn
from docx.oxml import OxmlElement
import os
import mimetypes
import subprocess
import jupyter_client
# import matlabengine  # Install with pip install matlab.engine
from memory_profiler import profile


class AdvancedPlusPlusEditor:
    def __init__(self, root):
        self.root = root
        self.root.title("Advanced++ Editor")
        self.text_area = scrolledtext.ScrolledText(root, wrap=tk.WORD)
        self.text_area.pack(fill=tk.BOTH, expand=True)
        self.line_numbers = scrolledtext.ScrolledText(root, wrap=tk.NONE, width=5)
        self.line_numbers.pack(fill=tk.Y, side=tk.LEFT)
        self.text_area.bind("<KeyRelease>", self.update_line_numbers)

        # Create a menu bar
        menubar = Menu(root)
        root.config(menu=menubar)

        # File menu
        file_menu = Menu(menubar, tearoff=0)
        menubar.add_cascade(label="File", menu=file_menu)
        file_menu.add_command(label="Open", command=self.open_file)
        file_menu.add_command(label="Save", command=self.save_file)
        file_menu.add_separator()
        file_menu.add_command(label="Exit", command=root.quit)

        # Edit menu
        edit_menu = Menu(menubar, tearoff=0)
        menubar.add_cascade(label="Edit", menu=edit_menu)
        edit_menu.add_command(label="Cut", command=self.cut_text)
        edit_menu.add_command(label="Copy", command=self.copy_text)
        edit_menu.add_command(label="Paste", command=self.paste_text)

        # Format menu
        format_menu = Menu(menubar, tearoff=0)
        menubar.add_cascade(label="Format", menu=format_menu)
        format_menu.add_command(label="Bold", command=self.toggle_bold)
        format_menu.add_command(label="Italic", command=self.toggle_italic)
        format_menu.add_command(label="Underline", command=self.toggle_underline)
        format_menu.add_command(label="Font Color", command=self.change_font_color)
        format_menu.add_command(label="Text Alignment", command=self.change_text_alignment)

        # Code menu
        code_menu = Menu(menubar, tearoff=0)
        menubar.add_cascade(label="Code", menu=code_menu)
        code_menu.add_command(label="Compile and Run", command=self.compile_and_run)
        code_menu.add_command(label="Run Python Kernel", command=self.run_python_kernel)
        #code_menu.add_command(label="Run Java Kernel", command=self.run_java_kernel)
        #code_menu.add_command(label="Run C++ Kernel", command=self.run_cpp_kernel)
        #code_menu.add_command(label="Run Bash Kernel", command=self.run_bash_kernel)
        # code_menu.add_command(label="Run MATLAB", command=self.run_matlab)

        # Help menu
        help_menu = Menu(menubar, tearoff=0)
        menubar.add_cascade(label="Help", menu=help_menu)
        help_menu.add_command(label="About", command=self.show_about_info)

        self.bold_on = False
        self.italic_on = False
        self.underline_on = False

        # Define a dictionary to map file extensions to Pygments lexers
        self.lexers = {
            ".py": "python",
            ".java": "java",
            ".cpp": "cpp",
            ".bash": "bash",
            # ".m": "matlab",
        }

    def open_file(self):
        file_path = filedialog.askopenfilename(filetypes=[("Text Files", "*.txt"), ("Word Documents", "*.docx")])
        if file_path:
            _, file_extension = os.path.splitext(file_path)
            if file_extension == ".docx":
                self.open_word_document(file_path)
            elif file_extension in self.lexers:
                self.open_code_file(file_path)
            else:
                self.open_text_file(file_path)

    def open_word_document(self, file_path):
        doc = Document(file_path)
        for paragraph in doc.paragraphs:
            for run in paragraph.runs:
                font = run.font
                if run.bold:
                    font.bold = True
                if run.italic:
                    font.italic = True
                if run.underline:
                    font.underline = True
                if run.font.color.rgb:
                    font.color.rgb = run.font.color.rgb
                if paragraph.alignment != WD_PARAGRAPH_ALIGNMENT.LEFT:
                    paragraph.alignment = WD_PARAGRAPH_ALIGNMENT.LEFT
            self.text_area.insert(tk.END, paragraph.text + "\n")

    def open_code_file(self, file_path):
        file_content = ""
        with open(file_path, "r") as file:
            file_content = file.read()
        self.text_area.insert(tk.END, file_content)

    def open_text_file(self, file_path):
        with open(file_path, "r") as file:
            text_content = file.read()
            self.text_area.insert(tk.END, text_content)

    def save_file(self):
        file_path = filedialog.asksaveasfilename(defaultextension=".txt", filetypes=[("Text Files", "*.txt")])
        if file_path:
            with open(file_path, "w") as file:
                file_content = self.text_area.get("1.0", tk.END)
                file.write(file_content)

    def cut_text(self):
        self.text_area.event_generate("<<Cut>>")

    def copy_text(self):
        self.text_area.event_generate("<<Copy>>")

    def paste_text(self):
        self.text_area.event_generate("<<Paste>>")

    def toggle_bold(self):
        if self.bold_on:
            self.text_area.tag_remove("bold", "sel.first", "sel.last")
        else:
            self.text_area.tag_add("bold", "sel.first", "sel.last")
        self.bold_on = not self.bold_on

    def toggle_italic(self):
        if self.italic_on:
            self.text_area.tag_remove("italic", "sel.first", "sel.last")
        else:
            self.text_area.tag_add("italic", "sel.first", "sel.last")
        self.italic_on = not self.italic_on

    def toggle_underline(self):
        if self.underline_on:
            self.text_area.tag_remove("underline", "sel.first", "sel.last")
        else:
            self.text_area.tag_add("underline", "sel.first", "sel.last")
        self.underline_on = not self.underline_on

    def change_font_color(self):
        color = tk.colorchooser.askcolor()[1]
        if color:
            self.text_area.tag_add("font_color", "sel.first", "sel.last")
            self.text_area.tag_configure("font_color", foreground=color)

    def change_text_alignment(self):
        alignment = simpledialog.askstring("Text Alignment", "Enter alignment (left, center, right):")
        if alignment and alignment in ["left", "center", "right"]:
            self.text_area.tag_add("alignment", "sel.first", "sel.last")
            self.text_area.tag_configure("alignment", justify=alignment)

    @profile  # Decorate the method you want to profile
    def run_python_kernel(self):
        code_content = self.text_area.get("1.0", tk.END)
        try:
            # Execute Python code while profiling memory usage
            exec(code_content)
        except Exception as e:
            messagebox.showerror("Python Error", f"An error occurred:\n{e}")

    def compile_and_run(self):
        code_content = self.text_area.get("1.0", tk.END)
        file_extension = self.get_file_extension()
        if file_extension == ".py":
            self.compile_and_run_python(code_content)
        elif file_extension == ".java":
            self.compile_and_run_java(code_content)
        elif file_extension == ".cpp":
            self.compile_and_run_cpp(code_content)
        elif file_extension == ".bash":
            self.run_bash_script(code_content)
        else:
            tk.messagebox.showinfo(
                "Unsupported Language", "Compilation and execution are not supported for this language."
            )

    def compile_and_run_python(self, code_content):
        try:
            # Execute Python code
            exec(code_content)
        except Exception as e:
            messagebox.showerror("Python Error", f"An error occurred:\n{e}")

    def compile_and_run_java(self, code_content):
        try:
            # Write Java code to a temporary file
            with open("Temp.java", "w") as java_file:
                java_file.write(code_content)
            # Compile the Java code
            compile_process = subprocess.Popen(
                ["javac", "Temp.java"],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                universal_newlines=True,
            )
            compile_stdout, compile_stderr = compile_process.communicate()
            if compile_process.returncode == 0:
                # Run the compiled Java program
                run_process = subprocess.Popen(
                    ["java", "Temp"],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    universal_newlines=True,
                )
                run_stdout, run_stderr = run_process.communicate()
                messagebox.showinfo("Java Output", run_stdout)
            else:
                messagebox.showerror("Java Compilation Error", compile_stderr)
        except Exception as e:
            messagebox.showerror("Java Error", f"An error occurred:\n{e}")

    def compile_and_run_cpp(self, code_content):
        try:
            # Write C++ code to a temporary file
            with open("Temp.cpp", "w") as cpp_file:
                cpp_file.write(code_content)
            # Compile and run the C++ code
            compile_process = subprocess.Popen(
                ["g++", "Temp.cpp", "-o", "Temp"],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                universal_newlines=True,
            )
            compile_stdout, compile_stderr = compile_process.communicate()
            if compile_process.returncode == 0:
                run_process = subprocess.Popen(
                    ["./Temp"],
                    stdout=subprocess.PIPE,
                    stderr=subprocess.PIPE,
                    universal_newlines=True,
                )
                run_stdout, run_stderr = run_process.communicate()
                messagebox.showinfo("C++ Output", run_stdout)
            else:
                messagebox.showerror("C++ Compilation Error", compile_stderr)
        except Exception as e:
            messagebox.showerror("C++ Error", f"An error occurred:\n{e}")

    def run_bash_script(self, code_content):
        try:
            # Write Bash script to a temporary file
            with open("Temp.sh", "w") as bash_file:
                bash_file.write(code_content)
            # Execute the Bash script
            run_process = subprocess.Popen(
                ["bash", "Temp.sh"],
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                universal_newlines=True,
            )
            run_stdout, run_stderr = run_process.communicate()
            messagebox.showinfo("Bash Output", run_stdout)
        except Exception as e:
            messagebox.showerror("Bash Error", f"An error occurred:\n{e}")

    #    def run_matlab(self):
    #        code_content = self.text_area.get("1.0", tk.END)
    #        try:
    #            eng = matlab.engine.start_matlab()
    #            eng.eval(code_content, nargout=0)
    #            eng.quit()
    #        except Exception as e:
    #            messagebox.showerror("MATLAB Error", f"An error occurred:\n{e}")

    def update_line_numbers(self, event):
        line_count = self.text_area.get("1.0", tk.END).count("\n")
        lines = "\n".join(str(i) for i in range(1, line_count + 2))
        self.line_numbers.config(state=tk.NORMAL)
        self.line_numbers.delete("1.0", tk.END)
        self.line_numbers.insert(tk.END, lines)
        self.line_numbers.config(state=tk.DISABLED)

    def show_about_info(self):
        about_text = (
            "Advanced++ Editor - Release 1.1\n"
            "Copyright 1983 - 2023 Morales Research Inc\n"
            "A simple text editor with support for editing Word documents, "
            "syntax highlighting for code, and more.\n"
            "Author: Abdon Morales Jr"
        )
        messagebox.showinfo("About", about_text)

    def get_file_extension(self):
        file_path = self.root.title()
        _, file_extension = os.path.splitext(file_path)
        return file_extension


def main():
    root = tk.Tk()
    app = AdvancedPlusPlusEditor(root)
    root.mainloop()


if __name__ == "__main__":
    main()
