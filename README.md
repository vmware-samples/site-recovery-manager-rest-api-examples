# site-recovery-manager-rest-api-examples

## 1. Prerequisites
REST API gateway Open API spec file in one of the formats - .json or .yaml

REST API client generator.

Online option - https://editor.swagger.io/

Maven option:

**swagger-codegen-maven-plugin**
```
<plugin>
   <groupId>io.swagger.codegen.v3</groupId>
   <artifactId>swagger-codegen-maven-plugin</artifactId>
   <version>3.0.32</version>
   <dependencies>
      <dependency>
         <groupId>com.github.jknack</groupId>
         <artifactId>handlebars</artifactId>
         <version>4.3.0</version>
      </dependency>
   </dependencies>
   <executions>
      <execution>
         <phase>generate-sources</phase>
         <goals>
            <goal>generate</goal>
         </goals>
         <configuration>
            <inputSpec>${swagger.definitions.url}</inputSpec>
            <language>java</language>
            <output>${swagger.output.dir}</output>
            <generateSupportingFiles>true</generateSupportingFiles>
            <generateApiTests>false</generateApiTests>
            <generateApiDocumentation>false</generateApiDocumentation>
            <generateModelTests>false</generateModelTests>
            <generateModelDocumentation>false</generateModelDocumentation>
            <environmentVariables>
               <io.swagger.v3.parser.util.RemoteUrl.trustAll>true</io.swagger.v3.parser.util.RemoteUrl.trustAll>
            </environmentVariables>
            <configOptions>
               <dateLibrary>legacy</dateLibrary>
               <useRuntimeException>true</useRuntimeException>
               <hideGenerationTimestamp>true</hideGenerationTimestamp>
            </configOptions>
         </configuration>
      </execution>
   </executions>
</plugin>
```
Other options are also available.

Project build tools - Maven, Gradle, etc.

## 2. Project Setup Steps
Generate REST API gateway client.

Build REST API gateway generated client.

Create your project and introduce dependency on REST API gateway client built in the previous step.

Write your code to start calling REST API gateway endpoints.

First start with login request in order to obtain REST API gateway session token. This needs Basic Authentication with user and password credentials for the local SRM/HMS site. Add the obtained session token to the header, with key "x-dr-session", of every future REST API request.

Call get all pairings request. This is a preliminary step before calling remote login request.

Call remote login request in order to authenticate to the SRM/HMS remote site. This needs Basic Authentication with user and password credentials for the remote SRM/HMS site.

Now logged in at the local and remote site, any request of your interest can be made.


With project build tool of your choice build your project.

## Contributing

The site-recovery-manager-rest-api-examples project team welcomes contributions from the community. Before you start working with site-recovery-manager-rest-api-examples, please
read our [Developer Certificate of Origin](https://cla.vmware.com/dco). All contributions to this repository must be
signed as described on that page. Your signature certifies that you wrote the patch or have the right to pass it on
as an open-source patch. For more detailed information, refer to [CONTRIBUTING.md](CONTRIBUTING.md).

## License

