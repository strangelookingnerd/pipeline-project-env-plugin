package io.jenkins.plugins.projectenv;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithTimeout;

public class WithProjectEnvStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Test
    public void testStepExecution() throws Exception {
        // Create a new Pipeline with the given (Scripted Pipeline) definition
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node {\n" +
                "  withProjectEnv(cliVersion: '3.4.0', cliDebug: true) {\n" +
                "    sh 'echo $(mvn -v)'\n" +
                "    sh 'echo $(java -version)'\n" +
                "  }\n" +
                "}", true));

        WorkflowRun run = jenkins.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0));
        jenkins.assertLogContains("Apache Maven 3.8.3", run);
        jenkins.assertLogContains("openjdk version \"1.8.0_312\"", run);
    }

}
