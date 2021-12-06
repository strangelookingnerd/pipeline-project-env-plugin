package io.jenkins.plugins.projectenv.agent;

import jenkins.security.MasterToSlaveCallable;
import org.apache.commons.lang.SystemUtils;

public class AgentInfoCallable extends MasterToSlaveCallable<AgentInfo, Exception> {

    @Override
    public AgentInfo call() throws Exception {
        OperatingSystem operatingSystem = getOperatingSystem();

        return ImmutableAgentInfo.builder().operatingSystem(operatingSystem).build();
    }

    private OperatingSystem getOperatingSystem() {
        if (SystemUtils.IS_OS_WINDOWS) {
            return OperatingSystem.WINDOWS;
        } else if (SystemUtils.IS_OS_MAC) {
            return OperatingSystem.MACOS;
        } else if (SystemUtils.IS_OS_LINUX) {
            return OperatingSystem.LINUX;
        } else {
            throw new IllegalStateException("unsupported OS " + SystemUtils.OS_NAME);
        }
    }

}
