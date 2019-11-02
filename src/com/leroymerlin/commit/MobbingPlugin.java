package com.leroymerlin.commit;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiManager;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class FileModificationEntry {
    String file = "";
    int line = 0;
    int column = 0;
    long lastModified = 0;
}

public class MobbingPlugin implements ProjectComponent, BulkFileListener {

    private static final String PLUGIN_NAME = "FileSyncPlugin";
    public static final String PLUGIN_DISPLAY_NAME = "File Synchronization";

    public static ArrayList<FileModificationEntry> modifiedFiles = new ArrayList<FileModificationEntry>();

    private MessageBusConnection connection;
    private Project project;


    public MobbingPlugin(Project project) {
        this.project = project;
        this.connection = ApplicationManager.getApplication().getMessageBus().connect();
    }

    public void initComponent() {
        EditorFactory.getInstance().getEventMulticaster().addDocumentListener(new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                Document document = event.getDocument();
                VirtualFile file = FileDocumentManager.getInstance().getFile(document);

                if (file == null) {
                    return;
                }

                int offset = event.getOffset();
                int newLength = event.getNewLength();

                // actual logic depends on which line we want to call 'changed' when '\n' is inserted
                int firstLine = document.getLineNumber(offset);
                int lastLine = newLength == 0 ? firstLine : document.getLineNumber(offset + newLength - 1);

                Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
                int columnNumber = editor.getCaretModel().getOffset() - document.getLineStartOffset(firstLine);

                boolean oldUpdated = false;

                FileModificationEntry oldFme = modifiedFiles.stream()
                        .filter(Objects::nonNull)
                        .filter(fme -> Objects.nonNull(file.getCanonicalFile()) && file.getCanonicalPath().equals(fme.file))
                        .findAny()
                        .orElse(null);

                if (oldFme != null) {
                    oldFme.line = firstLine;
                    oldFme.column = columnNumber;
                    oldFme.lastModified = System.currentTimeMillis() / 1000L;
                    oldUpdated = true;
                }
                else {
                    FileModificationEntry fme = new FileModificationEntry();
                    fme.file = file.getCanonicalPath();
                    fme.lastModified = System.currentTimeMillis() / 1000L;
                    fme.line = firstLine;
                    fme.column = columnNumber;

                    modifiedFiles.add(fme);
                }
            }
        });
    }

    public void disposeComponent() {
        connection.disconnect();
    }

    public void projectOpened() {
    }

    public void projectClosed() {
    }

    public void before(List<? extends VFileEvent> events) {
        for (VFileEvent fe : events) {
            if (fe instanceof VFileContentChangeEvent) {
                System.out.println("before(getCanonicalPath)->" + fe.getFile().getCanonicalPath());
                System.out.println("before(getName)->" + fe.getFile().getName());
                System.out.println("before(getLength)->" + fe.getFile().getLength());
            }
        }
    }

    public void after(List<? extends VFileEvent> events) {
        System.out.println("after" + events);
    }

    @NotNull
    public String getComponentName() {
        return PLUGIN_NAME + "Component";
    }

}