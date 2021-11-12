package io.jenkins.plugins.projectenv.toolinfo;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;

@Gson.TypeAdapters
@Value.Immutable
@Value.Style(jdkOnly = true)
public interface ToolInfo {

    Map<String, String> getEnvironmentVariables();

    List<String> getPathElements();

}
