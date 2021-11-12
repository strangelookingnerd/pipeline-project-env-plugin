package io.jenkins.plugins.projectenv.proc;

import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.TaskListener;
import io.jenkins.plugins.projectenv.context.StepContextHelper;
import org.jenkinsci.plugins.workflow.steps.StepContext;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public final class ProcHelper {

    private ProcHelper() {
        // noop
    }

    public static ProcResult executeAndReturnStdOut(ProcStarter procStarter, StepContext context) throws Exception {
        try (ByteArrayOutputStream stdOutInputStream = new ByteArrayOutputStream()) {

            Thread stdErrThread;
            int exitCode;
            try (PipedInputStream stdErrInputStream = new PipedInputStream();
                 BufferedOutputStream stdErrOutputStream = new BufferedOutputStream(new PipedOutputStream(stdErrInputStream))) {

                Proc process = procStarter
                        .stdout(stdOutInputStream)
                        .stderr(stdErrOutputStream)
                        .start();

                stdErrThread = attachStdErrLoggingThread(stdErrInputStream, context);
                exitCode = process.join();
            }
            stdErrThread.join();

            return ImmutableProcResult.builder()
                    .exitCode(exitCode)
                    .stdOutput(stdOutInputStream.toString(StandardCharsets.UTF_8.name()))
                    .build();
        }
    }

    private static Thread attachStdErrLoggingThread(InputStream stdErrInputStream, StepContext context) throws Exception {
        PrintStream logger = StepContextHelper.getOrThrow(context, TaskListener.class).getLogger();

        Thread thread = new Thread(() -> {
            try (Scanner scanner = new Scanner(stdErrInputStream, StandardCharsets.UTF_8.name())) {
                while (scanner.hasNextLine()) {
                    logger.println(scanner.nextLine());
                }
            }
        });
        thread.start();

        return thread;
    }

}
