package io.jenkins.plugins.projectenv;

import hudson.Extension;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.util.Collections;
import java.util.Set;

public class WithProjectEnvStep extends Step {

    private String cliVersion;
    private boolean cliDebug;
    private String configFile = "project-env.toml";

    @DataBoundConstructor
    public WithProjectEnvStep() {
        // noop
    }

    @DataBoundSetter
    public void setCliVersion(String cliVersion) {
        this.cliVersion = cliVersion;
    }

    @DataBoundSetter
    public void setCliDebug(boolean cliDebug) {
        this.cliDebug = cliDebug;
    }

    @DataBoundSetter
    public void setConfigFile(String configFile) {
        this.configFile = configFile;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        if (cliVersion != null) {
            return new WithProjectEnvStepExecution(stepContext, cliDebug, configFile, cliVersion);
        }

        return new WithProjectEnvStepExecution(stepContext, cliDebug, configFile);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        public String getFunctionName() {
            return "withProjectEnv";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }

    }

}
