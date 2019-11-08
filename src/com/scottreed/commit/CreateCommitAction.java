package com.scottreed.commit;

import com.google.gson.Gson;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vcs.CommitMessageI;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.ui.Refreshable;
import com.intellij.openapi.wm.WindowManager;
import git4idea.branch.GitBranchUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateCommitAction extends AnAction implements DumbAware {

    @Override
    public void actionPerformed(AnActionEvent actionEvent) {
        final CommitMessageI commitPanel = getCommitPanel(actionEvent);
        if (commitPanel == null)
            return;

        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        Project activeProject = null;
        for (Project project : projects) {
            Window window = WindowManager.getInstance().suggestParentWindow(project);
            if (window != null && window.isActive()) {
                activeProject = project;
            }
        }

        String branchName = GitBranchUtil.getCurrentRepository(activeProject).getCurrentBranch().getName();
        Pattern branchPattern = Pattern.compile("(SO-[0-9]*)");
        Matcher m = branchPattern.matcher(branchName);

        m.find();
        branchName = m.group(0);

        ArrayList<Object[]> workspaceStatusStringBuilder = new ArrayList<Object[]>();
        for (FileModificationEntry s : MobbingPlugin.modifiedFiles)
        {
            if ((System.currentTimeMillis() / 1000L) - s.lastModified < (60 * 15) && !s.file.equals("/Dummy.txt")) {
                Object[] arr = { s.relative_file, s.line, s.column };
                workspaceStatusStringBuilder.add(arr);
            }
        }
        String json = new Gson().toJson(workspaceStatusStringBuilder);

        commitPanel.setCommitMessage("[" + branchName + "] " + "╠" + json + "╣");
    }

    @Nullable
    private static CommitMessageI getCommitPanel(@Nullable AnActionEvent e) {
        if (e == null) {
            return null;
        }
        Refreshable data = Refreshable.PANEL_KEY.getData(e.getDataContext());
        if (data instanceof CommitMessageI) {
            return (CommitMessageI) data;
        }
        return VcsDataKeys.COMMIT_MESSAGE_CONTROL.getData(e.getDataContext());
    }
}
