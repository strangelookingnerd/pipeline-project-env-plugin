package io.jenkins.plugins.projectenv.toolinfo;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(jdkOnly = true)
public interface ToolInfo {

    Optional<String> getPrimaryExecutable();

    Map<String, String> getEnvironmentVariables();

    List<String> getPathElements();

    Map<String, String> getUnhandledProjectResources();

}
