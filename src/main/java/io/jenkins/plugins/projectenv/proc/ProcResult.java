package io.jenkins.plugins.projectenv.proc;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(jdkOnly = true)
public interface ProcResult {
    int getExitCode();

    String getStdOutput();
}
