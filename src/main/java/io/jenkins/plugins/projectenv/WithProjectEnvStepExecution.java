package io.jenkins.plugins.projectenv;

import hudson.EnvVars;
import io.projectenv.core.commons.archive.ArchiveExtractorFactory;
import io.projectenv.core.commons.download.DownloadUrlDictionary;
import io.projectenv.core.commons.download.DownloadUrlSubstitutorFactory;
import io.projectenv.core.commons.download.ImmutableDownloadUrlDictionary;
import io.projectenv.core.commons.process.ProcessHelper;
import io.projectenv.core.commons.process.ProcessResult;
import io.projectenv.core.commons.system.CPUArchitecture;
import io.projectenv.core.commons.system.OperatingSystem;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.steps.BodyExecutionCallback;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;
import org.jenkinsci.plugins.workflow.steps.GeneralNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class WithProjectEnvStepExecution extends GeneralNonBlockingStepExecution {

    private static final Logger LOGGER = Logger.getLogger(WithProjectEnvStepExecution.class.getName());

    private final String cliVersion;
    private final boolean cliDebug;

    public WithProjectEnvStepExecution(StepContext stepContext, String cliVersion, boolean cliDebug) throws IOException, InterruptedException {
        super(stepContext);

        this.cliVersion = cliVersion;
        this.cliDebug = cliDebug;
    }

    @Override
    public boolean start() throws Exception {
        run(this::execute);
        return false;
    }

    private void execute() throws Exception {
        File tempDir = createTempDirectory("project-env");

        File archive = downloadProjectEnvCliArchive(tempDir);
        extractProjectEnvCliArchive(archive, tempDir);

        File executable = resolveProjectEnvCliExecutable(tempDir);
        Map<String, List<ToolInfo>> allToolInfos = executeProjectEnvCli(executable);

        EnvVars projectEnvVars = processToolInfos(allToolInfos);
        BodyExecutionCallback callback = createTempDirectoryCleanupCallback(tempDir);

        invokeBodyWithEnvVarsAndCallback(projectEnvVars, callback);
    }

    private File createTempDirectory(String name) throws IOException {
        File temporaryFolder = File.createTempFile(name, StringUtils.EMPTY, null);
        FileUtils.forceDelete(temporaryFolder);
        FileUtils.forceMkdir(temporaryFolder);

        return temporaryFolder;
    }

    private File downloadProjectEnvCliArchive(File targetDirectory) throws IOException, URISyntaxException {
        String archiveUrl = createProjectEnvCliArchiveUrl();
        String archiveFilename = FilenameUtils.getName(archiveUrl);

        File targetFile = new File(targetDirectory, archiveFilename);
        downloadArchive(archiveUrl, targetFile);

        return targetFile;
    }

    private String createProjectEnvCliArchiveUrl() {
        DownloadUrlDictionary dictionary = ImmutableDownloadUrlDictionary.builder()
                .putParameters("VERSION", cliVersion)
                .putOperatingSystemSpecificParameters(
                        "OS",
                        createMapOf(
                                OperatingSystem.MACOS, "macos",
                                OperatingSystem.LINUX, "linux",
                                OperatingSystem.WINDOWS, "windows"
                        )
                )
                .putOperatingSystemSpecificParameters(
                        "FILE_EXT",
                        createMapOf(
                                OperatingSystem.MACOS, "tar.gz",
                                OperatingSystem.LINUX, "tar.gz",
                                OperatingSystem.WINDOWS, "zip"
                        )
                )
                .putCPUArchitectureSpecificParameters(
                        "CPU_ARCH",
                        createMapOf(
                                CPUArchitecture.X64, "amd64"
                        )
                )
                .build();

        return DownloadUrlSubstitutorFactory
                .createDownloadUrlVariableSubstitutor(dictionary)
                .replace("https://github.com/Project-Env/project-env-core/releases/download/v${VERSION}/cli-${VERSION}-${OS}-${CPU_ARCH}.${FILE_EXT}");
    }

    private <K, V> Map<K, V> createMapOf(K k1, V v1) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        return map;
    }

    private <K, V> Map<K, V> createMapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> map = new HashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        map.put(k3, v3);
        return map;
    }

    private void downloadArchive(String downloadUrl, File target) throws IOException, URISyntaxException {
        try (ReadableByteChannel inputChannel = Channels.newChannel(new URI(downloadUrl).toURL().openStream());
             FileChannel outputChannel = new FileOutputStream(target).getChannel()) {

            outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
        }
    }

    private void extractProjectEnvCliArchive(File archive, File target) throws IOException {
        ArchiveExtractorFactory.createArchiveExtractor().extractArchive(archive, target);
    }

    private File resolveProjectEnvCliExecutable(File sourceDirectory) {
        String executableFilename = "project-env-cli" + getExecutableExtension();
        File executable = new File(sourceDirectory, executableFilename);
        if (!executable.exists()) {
            throw new IllegalStateException("could not find Project-Env CLI at " + executable);
        }

        return executable;
    }

    private String getExecutableExtension() {
        return OperatingSystem.getCurrentOperatingSystem() == OperatingSystem.WINDOWS ? ".exe" : "";
    }

    private Map<String, List<ToolInfo>> executeProjectEnvCli(File executable) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add(executable.getAbsolutePath());
        commands.add("--config-file=project-env.toml");
        if (cliDebug) {
            commands.add("--debug");
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        ProcessResult processResult = ProcessHelper.executeProcess(processBuilder, true);

        return ToolInfoParser.fromJson(processResult.getStdOutput().orElse(null));
    }

    private EnvVars processToolInfos(Map<String, List<ToolInfo>> allToolInfos) {
        EnvVars envVars = new EnvVars();

        for (Map.Entry<String, List<ToolInfo>> entry : allToolInfos.entrySet()) {
            for (ToolInfo toolInfo : entry.getValue()) {
                List<File> pathElements = toolInfo.getPathElements();
                for (int i = 0; i < pathElements.size(); i++) {
                    File pathElement = pathElements.get(i);

                    envVars.put("PATH+" + StringUtils.upperCase(entry.getKey()) + "_" + i, pathElement.getAbsolutePath());
                }

                Map<String, File> environmentVariables = toolInfo.getEnvironmentVariables();
                for (Map.Entry<String, File> environmentVariableEntry : environmentVariables.entrySet()) {
                    envVars.put(environmentVariableEntry.getKey(), environmentVariableEntry.getValue().getAbsolutePath());
                }
            }
        }

        return envVars;
    }

    private BodyExecutionCallback createTempDirectoryCleanupCallback(File tempDirectory) {
        return new TailCall() {

            @Override
            protected void finished(StepContext context) throws Exception {
                deleteTempDirectory();
            }

            private void deleteTempDirectory() {
                try {
                    FileUtils.forceDelete(tempDirectory);
                } catch (IOException e) {
                    LOGGER.warning("failed to delete Project-Env temp directory " + tempDirectory.getAbsolutePath());
                }
            }

        };
    }

    private void invokeBodyWithEnvVarsAndCallback(EnvVars projectEnvVars, BodyExecutionCallback callback) throws IOException, InterruptedException {
        getContext()
                .newBodyInvoker()
                .withContexts(createEnvironmentExpander(projectEnvVars))
                .withCallback(callback)
                .start();
    }

    private EnvironmentExpander createEnvironmentExpander(EnvVars projectEnvVars) throws IOException, InterruptedException {
        return EnvironmentExpander
                .merge(getContext().get(EnvironmentExpander.class), new EnvironmentExpander() {
                    @Override
                    public void expand(@Nonnull EnvVars originalEnvVars) {
                        originalEnvVars.overrideAll(projectEnvVars);
                    }
                });
    }

}
