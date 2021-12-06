package io.jenkins.plugins.projectenv;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher.ProcStarter;
import hudson.Util;
import io.jenkins.plugins.projectenv.agent.AgentInfo;
import io.jenkins.plugins.projectenv.agent.AgentInfoCallable;
import io.jenkins.plugins.projectenv.agent.OperatingSystem;
import io.jenkins.plugins.projectenv.context.StepContextHelper;
import io.jenkins.plugins.projectenv.proc.ProcHelper;
import io.jenkins.plugins.projectenv.proc.ProcResult;
import io.jenkins.plugins.projectenv.toolinfo.ToolInfo;
import io.jenkins.plugins.projectenv.toolinfo.ToolInfoParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.GeneralNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import javax.annotation.Nonnull;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WithProjectEnvStepExecution extends GeneralNonBlockingStepExecution {

    private static final String PROJECT_ENV_CLI_DOWNLOAD_PATTERN = "https://github.com/Project-Env/project-env-core/releases/download/v{0}/cli-{0}-{1}-{2}.{3}";

    private static final String CLI_ARCHIVE_EXTENSION_TAR_GZ = "tar.gz";
    private static final String CLI_ARCHIVE_EXTENSION_ZIP = "zip";

    private static final String CLI_EXECUTABLE_FILE_NAME = "project-env-cli";

    private static final String CLI_EXECUTABLE_FILE_EXTENSION_WINDOWS = ".exe";
    private static final String CLI_EXECUTABLE_FILE_EXTENSION_OTHERS = StringUtils.EMPTY;

    private static final String CLI_TARGET_OS_WINDOWS = "windows";
    private static final String CLI_TARGET_OS_MACOS = "macos";
    private static final String CLI_TARGET_OS_LINUX = "linux";

    private static final String CLI_TARGET_ARCH_AMD_64 = "amd64";

    private final String cliVersion;
    private final boolean cliDebug;
    private final String configFile;

    public WithProjectEnvStepExecution(StepContext stepContext, String cliVersion, boolean cliDebug, String configFile) {
        super(stepContext);

        this.cliVersion = cliVersion;
        this.cliDebug = cliDebug;
        this.configFile = configFile;
    }

    @Override
    public boolean start() throws Exception {
        run(this::execute);
        return false;
    }

    private void execute() throws Exception {
        AgentInfo agentInfo = getAgentInfo();

        FilePath temporaryDirectory = createTemporaryDirectory();

        FilePath projectEnvCliArchive = downloadProjectEnvCliArchive(agentInfo, temporaryDirectory);
        extractProjectEnvCliArchive(projectEnvCliArchive, temporaryDirectory);

        FilePath executable = resolveProjectEnvCliExecutable(agentInfo, temporaryDirectory);
        Map<String, List<ToolInfo>> allToolInfos = executeProjectEnvCli(executable);

        EnvVars projectEnvVars = processToolInfos(allToolInfos);
        BodyExecutionCallback callback = createTempDirectoryCleanupCallback(temporaryDirectory);

        invokeBodyWithEnvVarsAndCallback(projectEnvVars, callback);
    }

    private AgentInfo getAgentInfo() throws Exception {
        return StepContextHelper.getComputer(getContext()).getChannel().call(new AgentInfoCallable());
    }

    private FilePath createTemporaryDirectory() throws Exception {
        FilePath temporaryDirectoryRoot = StepContextHelper.getTemporaryDirectory(getContext());
        String temporaryDirectoryName = generateTemporaryDirectoryName();

        return temporaryDirectoryRoot.child(temporaryDirectoryName);
    }

    private String generateTemporaryDirectoryName() {
        return "withProjectEnv" + Util.getDigestOf(UUID.randomUUID().toString()).substring(0, 8);
    }

    private FilePath downloadProjectEnvCliArchive(AgentInfo agentInfo, FilePath targetDirectory) throws Exception {
        String archiveUrl = createProjectEnvCliArchiveUrl(agentInfo);
        String archiveFilename = FilenameUtils.getName(archiveUrl);

        FilePath targetFile = targetDirectory.child(archiveFilename);
        targetFile.copyFrom(new URI(archiveUrl).toURL());

        return targetFile;
    }

    private String createProjectEnvCliArchiveUrl(AgentInfo agentInfo) {
        String cliTargetOs = getCliTargetOs(agentInfo);
        String cliArchiveExtension = getCliArchiveExtension(agentInfo);
        String cliTargetArchitecture = getCliTargetArchitecture();

        return MessageFormat.format(PROJECT_ENV_CLI_DOWNLOAD_PATTERN, cliVersion, cliTargetOs, cliTargetArchitecture, cliArchiveExtension);
    }

    private String getCliTargetOs(AgentInfo agentInfo) {
        OperatingSystem operatingSystem = agentInfo.getOperatingSystem();
        switch (operatingSystem) {
            case WINDOWS:
                return CLI_TARGET_OS_WINDOWS;
            case MACOS:
                return CLI_TARGET_OS_MACOS;
            case LINUX:
                return CLI_TARGET_OS_LINUX;
            default:
                throw new IllegalArgumentException("unexpected value " + operatingSystem + " received");
        }
    }

    private String getCliArchiveExtension(AgentInfo agentInfo) {
        OperatingSystem operatingSystem = agentInfo.getOperatingSystem();
        switch (operatingSystem) {
            case WINDOWS:
                return CLI_ARCHIVE_EXTENSION_ZIP;
            case MACOS:
            case LINUX:
                return CLI_ARCHIVE_EXTENSION_TAR_GZ;
            default:
                throw new IllegalArgumentException("unexpected value " + operatingSystem + " received");
        }
    }

    private String getCliTargetArchitecture() {
        return CLI_TARGET_ARCH_AMD_64;
    }

    private void extractProjectEnvCliArchive(FilePath archive, FilePath target) throws Exception {
        if (StringUtils.endsWith(archive.getName(), CLI_ARCHIVE_EXTENSION_TAR_GZ)) {
            archive.untar(target, FilePath.TarCompression.GZIP);
        } else {
            archive.unzip(target);
        }
    }

    private FilePath resolveProjectEnvCliExecutable(AgentInfo agentInfo, FilePath sourceDirectory) throws Exception {
        String executableFilename = CLI_EXECUTABLE_FILE_NAME + getExecutableExtension(agentInfo);
        FilePath executable = sourceDirectory.child(executableFilename);
        if (!executable.exists()) {
            throw new IllegalStateException("could not find Project-Env CLI at " + executable);
        }

        return executable;
    }

    private String getExecutableExtension(AgentInfo agentInfo) {
        return agentInfo.getOperatingSystem() == OperatingSystem.WINDOWS ?
                CLI_EXECUTABLE_FILE_EXTENSION_WINDOWS : CLI_EXECUTABLE_FILE_EXTENSION_OTHERS;
    }

    private Map<String, List<ToolInfo>> executeProjectEnvCli(FilePath executable) throws Exception {
        List<String> commands = createProjectEnvCliCommand(executable);
        FilePath workspace = StepContextHelper.getWorkspace(getContext());

        ProcStarter procStarter = StepContextHelper.getLauncher(getContext())
                .launch()
                .cmds(commands)
                .pwd(workspace);

        ProcResult procResult = ProcHelper.executeAndReturnStdOut(procStarter, getContext());
        if (procResult.getExitCode() != 0) {
            throw new IllegalStateException("received non-zero exit code " + procResult.getExitCode() + " from Project-Env CLI");
        }

        return ToolInfoParser.fromJson(procResult.getStdOutput());
    }

    private List<String> createProjectEnvCliCommand(FilePath executable) {
        List<String> command = new ArrayList<>();
        command.add(executable.getRemote());
        command.add("--config-file=" + configFile);
        if (cliDebug) {
            command.add("--debug");
        }

        return command;
    }

    private EnvVars processToolInfos(Map<String, List<ToolInfo>> allToolInfos) {
        EnvVars envVars = new EnvVars();

        for (Map.Entry<String, List<ToolInfo>> entry : allToolInfos.entrySet()) {
            for (ToolInfo toolInfo : entry.getValue()) {
                List<String> pathElements = toolInfo.getPathElements();
                for (int i = 0; i < pathElements.size(); i++) {
                    String pathElement = pathElements.get(i);

                    envVars.put("PATH+" + StringUtils.upperCase(entry.getKey()) + "_" + i, pathElement);
                }

                envVars.putAll(toolInfo.getEnvironmentVariables());
            }
        }

        return envVars;
    }

    private BodyExecutionCallback createTempDirectoryCleanupCallback(FilePath tempDirectory) {
        return new TailCall() {
            @Override
            protected void finished(StepContext context) throws Exception {
                tempDirectory.deleteRecursive();
            }
        };
    }

    private void invokeBodyWithEnvVarsAndCallback(EnvVars projectEnvVars, BodyExecutionCallback callback) throws Exception {
        getContext()
                .newBodyInvoker()
                .withContexts(createEnvironmentExpander(projectEnvVars))
                .withCallback(callback)
                .start();
    }

    private EnvironmentExpander createEnvironmentExpander(EnvVars projectEnvVars) throws Exception {
        return EnvironmentExpander
                .merge(getContext().get(EnvironmentExpander.class), new EnvironmentExpander() {
                    @Override
                    public void expand(@Nonnull EnvVars originalEnvVars) {
                        originalEnvVars.overrideAll(projectEnvVars);
                    }
                });
    }

}