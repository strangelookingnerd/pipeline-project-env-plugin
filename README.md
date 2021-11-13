# Pipeline Project-Env Plugin

This plugin allows you to use Project-Env within Jenkins pipelines. See [Project-Env](https://project-env.github.io/) for more details.

## Usage

```groovy
node {
    withProjectEnv(cliVersion: '<cli version>', cliDebug: <true|false>, configFile: '<path to project-env.toml>') {
        ...
    }
}
```

| Option       | Mandatory | Description                                                                                                       |
|--------------|-----------|-------------------------------------------------------------------------------------------------------------------|
| `cliVersion` | Yes       | The Project-Env CLI version which should be used.                                                                 |
| `cliDebug`   | No        | Whether to activate the debug mode in the Project-Env CLI. If not configured, the debug mode will be deactivated. |
| `configFile` | No        | The path to the Project-Env CLI configuration file. If not configured, `project-env.toml` will be used.           |

### Example

```groovy
node {
    withProjectEnv(cliVersion: '3.4.0') {
        ...
    }
}
```

