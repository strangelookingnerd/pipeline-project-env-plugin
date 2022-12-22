# Pipeline Project-Env Plugin

[![Build Status](https://ci.jenkins.io/job/Plugins/job/pipeline-project-env-plugin/job/main/badge/icon)](https://ci.jenkins.io/job/Plugins/job/pipeline-project-env-plugin/job/main/)
[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/pipeline-project-env.svg)](https://plugins.jenkins.io/pipeline-project-env)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/pipeline-project-env.svg?color=blue)](https://plugins.jenkins.io/pipeline-project-env)

## Introduction

This plugin allows you to use Project-Env within Jenkins pipelines. See [Project-Env](https://project-env.github.io/) for more details.

## Getting started

```groovy
node {
    withProjectEnv(
            // The Project-Env CLI version which should be used.
            // If not configured, the latest version will be resolved automatically.
            cliVersion: string,
            // Whether to activate the debug mode in the Project-Env CLI. 
            // If not configured, the debug mode will be deactivated.
            cliDebug: boolean,
            // The path to the Project-Env CLI configuration file. 
            // If not configured, project-env.toml will be used.
            configFile: string
    ) {
        // ...
    }
}
```

### Example

```groovy
node {
    withProjectEnv(cliVersion: '3.4.0') {
        // ...
    }
}
```

## Contributing

See [contribution guidelines](https://github.com/jenkinsci/.github/blob/master/CONTRIBUTING.md)

## Changelog

See [releases](https://github.com/jenkinsci/pipeline-project-env-plugin/releases)

