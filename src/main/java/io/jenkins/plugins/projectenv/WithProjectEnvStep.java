package io.jenkins.plugins.projectenv;

import hudson.EnvVars;
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

    private final String cliVersion;
    private boolean cliDebug;

    @DataBoundConstructor
    public WithProjectEnvStep(String cliVersion) {
        this.cliVersion = cliVersion;
    }

    @DataBoundSetter
    public void setCliDebug(boolean cliDebug) {
        this.cliDebug = cliDebug;
    }

    @Override
    public StepExecution start(StepContext stepContext) throws Exception {
        return new WithProjectEnvStepExecution(stepContext, cliVersion, cliDebug);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(EnvVars.class);
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
