# SPReg (Perun) -> MitreID sync
Command-line tool for synchronization of data stored about the clients in Perun to MitreID. 
Serves as one-shot synchronization. To run it periodically, we advise to create a cron job.

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

### CONNECTION TO PERUN RPC ###
rpc:
  perunUrl: "https://perun-dev.cesnet.cz/ba/rpc"
  perunUser: "user"
  perunPassword: "pass"

### MAPPING OF CLIENT PROPERTIES TO PERUN ATTRIBUTES ###
attributes:
  proxy_identifier: "urn:perun:facility:attribute-def:def:proxyIdentifiers"
  is_test_sp: "urn:perun:facility:attribute-def:def:isTestSp"
  is_oidc: "urn:perun:facility:attribute-def:def:isOidc"
  manager_groups_id: "urn:perun:facility:attribute-def:def:managersGroupId"
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
  response_types: "urn:perun:facility:attribute-def:def:OIDCResponseTypes"
  introspection: "urn:perun:facility:attribute-def:def:OIDCAllowIntrospection"
  post_logout_redirect_uris: "urn:perun:facility:attribute-def:def:OIDCPostLogoutRedirectURIs"
  issue_refresh_tokens: "urn:perun:facility:attribute-def:def:OIDCIssueRefreshTokens"

### CONFIGURATION FOR SOME COMMON THINGS ###
conf:
  ### LANGUAGES SUPPORTED BY THE ENVIRONMENT - USED IN CLIENT NAME ETC ###
  langs: ["en", "cs"] 
  encryption_secret: "secret_goes_here"
  proxy_identifier_value: "https://login.cesnet.cz/proxy/"
  ### VO_ID, PARENT_GROUP_ID AND PARENT_GROUP_NAME FOR GROUPS CONTAINING MANAGERS ###
  managers_group_vo_id: 1
  managers_group_parent_group_id: 1
  managers_group_parent_group_name: "Facility managers"

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
  * to_perun
  * to_oidc
* INTERACTIVE: if set to TRUE, each action has to be confirmed by the user
  * true
  * false
