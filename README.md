# Pipeline Project-Env Plugin

This plugin allows you to use Project-Env within Jenkins pipelines. See https://project-env.github.io for more details.

## Usage

```groovy
node {
    withProjectEnv(cliVersion: '3.4.0', cliDebug: true) {
        sh 'echo $(mvn -v)'
        sh 'echo $(java -version)'
    }
}
```