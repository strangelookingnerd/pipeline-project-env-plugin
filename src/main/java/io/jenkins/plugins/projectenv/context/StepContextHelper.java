package io.jenkins.plugins.projectenv.context;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.util.Optional;

public class StepContextHelper {

    public static Computer getComputer(StepContext stepContext) throws Exception {
        return getOrThrow(stepContext, Computer.class);
    }

    public static Launcher getLauncher(StepContext stepContext) throws Exception {
        return getOrThrow(stepContext, Launcher.class);
    }

    public static EnvVars getEnvVars(StepContext stepContext) throws Exception {
        return getOrThrow(stepContext, EnvVars.class);
    }

    public static TaskListener getTaskListener(StepContext stepContext) throws Exception {
        return getOrThrow(stepContext, TaskListener.class);
    }

    public static FilePath getWorkspace(StepContext stepContext) throws Exception {
        FilePath workspace = getOrThrow(stepContext, FilePath.class);
        if (!workspace.exists()) {
            workspace.mkdirs();
        }

        return workspace;
    }

    public static FilePath getTemporaryDirectory(StepContext stepContext) throws Exception {
        FilePath workspace = getWorkspace(stepContext);

        return Optional.ofNullable(WorkspaceList.tempDir(workspace))
                .orElseThrow(() -> new IllegalStateException("failed to resolve temporary directory from context"));
    }

    private static <T> T getOrThrow(StepContext stepContext, Class<T> type) throws Exception {
        return Optional.ofNullable(stepContext.get(type))
                .orElseThrow(() -> new IllegalStateException("failed to resolve " + type.getSimpleName() + " from context"));
    }

}
