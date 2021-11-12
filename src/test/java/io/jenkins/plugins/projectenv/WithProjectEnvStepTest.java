package io.jenkins.plugins.projectenv;

import hudson.model.Label;
import hudson.model.Result;
import org.apache.commons.io.IOUtils;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithTimeout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

public class WithProjectEnvStepTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    @Before
    public void startSlave() throws Exception {
        jenkins.createSlave(Label.get("slave"));
    }

    @Test
    @WithTimeout(600)
    public void testStepExecution() throws Exception {
        String projectEnvConfigFileContent = readTestResource("project-env.toml");

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node('slave') {\n" +
                "  writeFile text: '''" + projectEnvConfigFileContent + "''', file: 'project-env.toml'\n" +
                "  withProjectEnv(cliVersion: '3.4.0', cliDebug: true) {\n" +
                "    sh 'java -version'\n" +
                "    sh 'native-image --version'\n" +
                "    sh 'mvn --version'\n" +
                "    sh 'gradle --version'\n" +
                "    sh 'node --version'\n" +
                "    sh 'yarn --version'\n" +
                "  }\n" +
                "}", true));

        WorkflowRun run = jenkins.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0));
        assertThat(run.getLog())
                // assert that the JDK (including native-image)  has been installed
                .contains("installing jdk...")
                .contains("openjdk version \"11.0.9\" 2020-10-20")
                .contains("GraalVM Version 20.3.0 (Java Version 11.0.9+10-jvmci-20.3-b06)")
                // assert that Maven has been installed
                .contains("installing maven...")
                .contains("Apache Maven 3.6.3 (cecedd343002696d0abb50b32b541b8a6ba2883f)")
                // assert that Gradle has been installed
                .contains("installing gradle...")
                .contains("Gradle 6.7.1")
                // assert that NodeJS (including yarn) has been installed
                .contains("installing nodejs...")
                .contains("v14.15.3")
                .contains("1.22.17");
    }

    @Test
    @WithTimeout(600)
    public void testStepExecutionWithCustomConfigFileLocation() throws Exception {
        String projectEnvConfigFileContent = readTestResource("project-env-empty.toml");

        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node('slave') {\n" +
                "  writeFile text: '" + projectEnvConfigFileContent + "', file: 'etc/project-env.toml'\n" +
                "  withProjectEnv(cliVersion: '3.4.0', cliDebug: true, configFile: 'etc/project-env.toml') {\n" +
                "  }\n" +
                "}", true));

        jenkins.assertBuildStatus(Result.SUCCESS, project.scheduleBuild2(0));
    }

    @Test
    @WithTimeout(600)
    public void testStepExecutionWithNonExistingConfigFile() throws Exception {
        WorkflowJob project = jenkins.createProject(WorkflowJob.class);
        project.setDefinition(new CpsFlowDefinition("" +
                "node('slave') {\n" +
                "  withProjectEnv(cliVersion: '3.4.0', cliDebug: true) {\n" +
                "  }\n" +
                "}", true));

        WorkflowRun run = jenkins.assertBuildStatus(Result.FAILURE, project.scheduleBuild2(0));
        assertThat(run.getLog()).contains("failed to install tools: FileNotFoundException");
    }

    private String readTestResource(String resource) throws IOException {
        return IOUtils.toString(getClass().getResource(resource), StandardCharsets.UTF_8);
    }

}
