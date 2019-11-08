package com.leroymerlin.commit;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.VcsFullCommitDetails;
import git4idea.GitCommit;
import git4idea.branch.GitBranchUtil;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;

import java.awt.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogParseAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent event) {
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project activeProject = null;
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                activeProject = project;
            }
        }
        GitRepository repository = GitBranchUtil.getCurrentRepository(activeProject);
        VirtualFile root = repository.getRoot();

        List<GitCommit> branchHistory = null;
        try {
            branchHistory = GitHistoryUtils.history(activeProject, root, repository.getCurrentBranch().getName());
        } catch (VcsException e) {
            e.printStackTrace();
        }

        // get hash of last commit
        String gitCommitMessage = branchHistory.get(0).getFullMessage();

        Pattern branchPattern = Pattern.compile("╠(.*)╣");
        Matcher m = branchPattern.matcher(gitCommitMessage);

        m.find();
        String rawJson = m.group(0);
        rawJson = rawJson.substring(1, rawJson.length() - 1);

        ArrayList<ArrayList<Object>> data = new Gson().fromJson(rawJson, ArrayList.class);

        Collections.reverse(data);
        for (ArrayList<Object> fme: data) {
            System.out.println(fme);
            String fileName = fme.get(0).toString();
            double lineNumber = (double) fme.get(1);
            double columnNumber = (double) fme.get(2);

            VirtualFile file = LocalFileSystem.getInstance().findFileByPath(activeProject.getBasePath() + "/" + fileName);
            FileEditor[] fe = FileEditorManager.getInstance(activeProject).openFile(file, true);

            Editor editor = FileEditorManager.getInstance(activeProject).getSelectedTextEditor();
            CaretModel caretModel = editor.getCaretModel();

            caretModel.moveToLogicalPosition(new LogicalPosition((int) lineNumber,(int) columnNumber));

            ScrollingModel scrollingModel = editor.getScrollingModel();
            scrollingModel.scrollToCaret(ScrollType.CENTER);
        }
    }
}

class FileModification {
    String file;
    int lineNumber;
    int column;

    FileModification(String file, int lineNumber, int column) {
        this.file = file;
        this.lineNumber = lineNumber;
        this.column = column;
    }
}