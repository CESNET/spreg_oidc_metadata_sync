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
  client_id: "urn:perun:facility:attribute-def:def:OIDCClientID"
  client_secret: "urn:perun:facility:attribute-def:def:OIDCClientSecret"
  name: "urn:perun:facility:attribute-def:def:serviceName"
  description: "urn:perun:facility:attribute-def:def:serviceDescription"
  redirect_uris: "urn:perun:facility:attribute-def:def:OIDCRedirectURIs"
  privacy_policy: "urn:perun:facility:attribute-def:def:privacyPolicyURL"
  contacts: "urn:perun:facility:attribute-def:def:administratorContact"
  scopes: "urn:perun:facility:attribute-def:def:requiredScopes"
  grant_types: "urn:perun:facility:attribute-def:def:OIDCGrantTypes"
  response_types: "urn:perun:facility:attribute-def:def:OIDCResponseTypes"
  introspection: "urn:perun:facility:attribute-def:def:OIDCAllowIntrospection"
  post_logout_redirect_uris: "urn:perun:facility:attribute-def:def:OIDCPostLogoutRedirectURIs"
  issue_refresh_tokens: "urn:perun:facility:attribute-def:def:OIDCIssueRefreshTokens"

### SECRET STRING FOR DECRYPTING ENCRYPTED DATA (i.e. ClientSecret) ###
encryption_secret: "8dDrjSXUUHLde5bY"

### PROXY IDENTIFIER ATTRIBUTE AND VALUE USED FOR SEARCHING ###
### FOR THE FACILITIES REPRESENTING THE CLIENT ###
proxy_identifier:
  attr: "urn:perun:facility:attribute-def:def:proxyIdentifiers"
  value: "https://login.cesnet.cz/idp/"

### CONFIGURATION OF SYNC ACTIONS ###
### ENABLE ACTIONS AND SPECIFY PROTECTED MitreID CLIENTS ###
actions:
  create: true
  update: true
  delete: true
  protected_client_ids:
    # LIST OF CLIENT_IDS IN MITRE DB THAT WILL NOT BE MODIFIED IN ANY WAY #
    - "client-id-1"
    - "client-id-2"
```

### Running
First, package the application. If you have already downloaded the compiled JAR file, skip to the next step.
```bash 
./mvnw spring-boot:repackage
```

Run the JAR with options to specify config file location and name (name defaults to application.yml)
```bash
java -jar PATH/TO/RUNNABLE/JAR/FILE.jar --spring.config.location=/PATH/TO/DIR/WITH/CONFIG/
 --spring.config.name=FILE_NAME_WITHOUT_EXTENSION
```