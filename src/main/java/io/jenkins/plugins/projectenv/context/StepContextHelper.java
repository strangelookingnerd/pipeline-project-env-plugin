package io.jenkins.plugins.projectenv.context;

import hudson.FilePath;
import hudson.model.Computer;
import hudson.slaves.WorkspaceList;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.util.Optional;

public class StepContextHelper {

    public static <T> T getOrThrow(StepContext stepContext, Class<T> type) throws Exception {
        return Optional.ofNullable(stepContext.get(type))
                .orElseThrow(() -> new IllegalStateException("failed to resolve " + type.getSimpleName() + " from context"));
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

    public static OperatingSystem getOperatingSystem(StepContext context) throws Exception {
        Computer computer = StepContextHelper.getOrThrow(context, Computer.class);
        if (Boolean.FALSE.equals(computer.isUnix())) {
            return OperatingSystem.WINDOWS;
        }

        String path = computer.getEnvironment().get("PATH");
        if (StringUtils.contains(path, "/Library/Apple")) {
            return OperatingSystem.MACOS;
        }

        return OperatingSystem.LINUX;
    }

}
