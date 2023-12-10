package io.jenkins.plugins.projectenv.agent;

import org.immutables.value.Value;

import java.io.Serializable;

@Value.Immutable
@Value.Style(jdkOnly = true)
public interface AgentInfo extends Serializable {

    OperatingSystem getOperatingSystem();
    String  getLineSeparator();

}
