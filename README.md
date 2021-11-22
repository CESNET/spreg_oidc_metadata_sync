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
```yaml
---
### LOGGING LEVELS ###
logging:
  level:
    cz.muni.ics: DEBUG
    org.springframework: INFO

### JDBC CONNECTION TO OIDC DB ###
jdbc:
  username: "database_user"
  password: "password"
  url: "jdbc:mariadb://localhost:3306/oidcdb"
  driver_class_name: "org.mariadb.jdbc.Driver"
  platform: "org.eclipse.persistence.platform.database.MySQLPlatform"

### CONNECTION TO PERUN RPC ###
rpc:
  perunUrl: "https://perun-dev.cesnet.cz/ba/rpc"
  perunUser: "user"
  perunPassword: "pass"

### MAPPING OF CLIENT PROPERTIES TO PERUN ATTRIBUTES ###
attributes:
  client_id: "urn:perun:facility:attribute-def:def:OIDCClientID"
  client_secret: "urn:perun:facility:attribute-def:def:OIDCClientSecret"
  name: "urn:perun:facility:attribute-def:def:serviceName"
  description: "urn:perun:facility:attribute-def:def:serviceDescription"
  redirect_uris: "urn:perun:facility:attribute-def:def:OIDCRedirectURIs"
  privacy_policy: "urn:perun:facility:attribute-def:def:privacyPolicyURL"
  contacts:
    - "urn:perun:facility:attribute-def:def:administratorContact"
  scopes: "urn:perun:facility:attribute-def:def:requiredScopes"
  grant_types: "urn:perun:facility:attribute-def:def:OIDCGrantTypes"
  code_challenge_type: "urn:perun:facility:attribute-def:def:OIDCCodeChallengeType"
  introspection: "urn:perun:facility:attribute-def:def:OIDCAllowIntrospection"
  post_logout_redirect_uris: "urn:perun:facility:attribute-def:def:OIDCPostLogoutRedirectURIs"
  issue_refresh_tokens: "urn:perun:facility:attribute-def:def:OIDCIssueRefreshTokens"
  proxy_identifier: "urn:perun:facility:attribute-def:def:proxyIdentifiers"
  master_proxy_identifier: "urn:perun:facility:attribute-def:def:masterProxyIdentifier"
  is_test_sp: "urn:perun:facility:attribute-def:def:isTestSp"
  is_oidc: "urn:perun:facility:attribute-def:def:isOidc"
  manager_groups_id: "urn:perun:facility:attribute-def:def:managersGroupId"
  home_page_uris:
    - "urn:perun:facility:attribute-def:def:informationURL"

### CONFIGURATION FOR SOME COMMON THINGS ###
conf:
  ### LANGUAGES SUPPORTED BY THE ENVIRONMENT - USED IN CLIENT NAME ETC ###
  langs: ["en", "cs"] 
  encryption_secret: "secret_goes_here"
  proxy_identifier_value: "https://login.cesnet.cz/proxy/"
  ### TOKEN TIMEOUTS - IF UNSPECIFIED, CURRENT VALUES ARE KEPT ###
  access_token_timeout: 28800 # default is 3600
  id_token_timeout: 600 # default is 600
  refresh_token_timeout: 604800 # default is 0
  ### VO_ID, PARENT_GROUP_ID AND PARENT_GROUP_NAME FOR GROUPS CONTAINING MANAGERS ###
  managers_group_vo_id: 1
  managers_group_parent_group_id: 1
  managers_group_parent_group_name: "Facility managers"
  ### OUTPUT FOR PROBE CHECK ####
  probe_output_file_location: "/etc/mitreid/check_probe_result.txt"

### CONFIGURATION OF SYNC ACTIONS ###
### ENABLE ACTIONS AND SPECIFY PROTECTED MitreID CLIENTS ###
actions:
  to_oidc:
    create: TRUE
    update: TRUE
    delete: FALSE
  to_perun:
    create: TRUE
    update: TRUE
    delete: FALSE
  protected_client_ids:
    # LIST OF CLIENT_IDS IN MITRE DB THAT WILL NOT BE MODIFIED IN ANY WAY #
    - "client-id-1"
    - "client-id-2"
```

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
