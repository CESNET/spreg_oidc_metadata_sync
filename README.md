# SPReg (Perun) -> MitreID sync
[![Checkstyle](https://github.com/CESNET/spreg_oidc_metadata_sync/actions/workflows/checkstyle.yml/badge.svg)](https://github.com/CESNET/spreg_oidc_metadata_sync/actions/workflows/checkstyle.yml)

[![Maven build](https://github.com/CESNET/spreg_oidc_metadata_sync/actions/workflows/maven.yml/badge.svg)](https://github.com/CESNET/spreg_oidc_metadata_sync/actions/workflows/maven.yml)

Command-line tool for synchronization of data stored about the clients in Perun to MitreID. 
Serves as one-shot synchronization. To run it periodically, we advise to create a cron job.

## Contribution

This repository uses [Conventional Commits](https://www.npmjs.com/package/@commitlint/config-conventional).
Any change that significantly changes behavior in a backward-incompatible way or requires a configuration change must be marked as BREAKING CHANGE.

### Available scopes:
* script

### Configuration

See [example configuration](src/main/resources/application.yml).

### Running
First, package the application. If you have already downloaded the compiled JAR file, skip to the next step.
```bash 
./mvnw clean package -Dfinal.name=oidc-sync-v1
```
#### Arguments:
* final.name: name of the built jar

Run the JAR with options to specify config file location and name (name defaults to application.yml)
```bash
java -jar PATH/TO/RUNNABLE/JAR/FILE.jar --spring.config.location=/PATH/TO/DIR/WITH/CONFIG/
 --spring.config.name=FILE_NAME_WITHOUT_EXTENSION --mode=to_perun --interactive=true
```
#### Arguments:
* MODE: specifies the destination to which the sync will be performed
  * to\_perun
  * to\_oidc
* INTERACTIVE: if set to TRUE, each action has to be confirmed by the user
  * true
  * false
