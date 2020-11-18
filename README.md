# Metadata writer
Writes Alfresco metadata to files.

## Using the metadata writer

* Include the module in your project
```xml
    <dependency>
      <groupId>org.redpill-linpro.alfresco.module</groupId>
      <artifactId>metadatawriter</artifactId>
      <version>5.0.0-SNAPSHOT</version>
      <exclusions>
        <exclusion>
          <groupId>org.bouncycastle</groupId>
          <artifactId>bctsp-jdk14</artifactId>
        </exclusion>
      </exclusions>
    </dependency>
```
* Define a bean in a -context.xml file:
```xml
<bean id="my.metadata.service.id" class="org.redpill.alfresco.module.metadatawriter.services.impl.MetadataServiceImpl" parent="metadata-writer.abstract.service">
      <property name="serviceName" value="my.metadata.service" />
      <property name="converters">
        <list>
          <ref bean="my.metadata-writer.dateConverter" />
        </list>
      </property>
      <property name="mappings">
        <props>
          <prop key="Property1">custom:myProp1</prop>
          <prop key="Property2">custom:myProp2</prop>
          <prop key="Property3">custom:myProp3</prop>
        </props>
      </property>
 </bean>
```
* In the 'mappings' property, you define what properties you want to be written
  on the document. If they exist on the document, they will be written, other properties are ignored.

* In the 'serviceName' property, you define the name of your custom metadata service.
 
* In the 'converters' property, you define a list of converter that can be used to format or convert property values.
 
* To create custom converters you need two things, a class implementing 'ValueConverter' and 
 a bean-definition for that class. Then you can register your custom converter in a metadataService bean.
 
* To indicate that a documents metadata should be written, add the 'mdw:serviceName' and set the value
 to be the 'serviceName' of your custom metadataService. In the example this would be 'my.metadata.service'.

# Known issues
ODF documents does not work

# Compatibility

This version is made for ACS 6.2.x and later. Verified to work with JDK 11 and ACS 6.2.

Can handle formats:
* doc, docx
* xls, xlsx
* ppt, pptx
* pdf

# Developer instructions

This is an ACS project for Alfresco SDK 4.0.

Download java11-openjdk-dcevm-linux.tar.gz from https://github.com/TravaOpenJDK/trava-jdk-11-dcevm/releases and put into the src/main/docker/ folder.

Run with `./run.sh build_start` or `./run.bat build_start` and verify that it

 * Runs Alfresco Content Service (ACS)
 * (Optional) Runs Alfresco Share
 * Runs Alfresco Search Service (ASS)
 * Runs PostgreSQL database
 * Deploys the JAR assembled module
 
All the services of the project are now run as docker containers. The run script offers the next tasks:

 * `build_start`. Build the whole project, recreate the ACS docker image, start the dockerised environment composed by ACS, Share (optional), ASS 
 and PostgreSQL and tail the logs of all the containers.
 * `build_start_it_supported`. Build the whole project including dependencies required for IT execution, recreate the ACS docker image, start the dockerised environment 
 composed by ACS, Share (optional), ASS and PostgreSQL and tail the logs of all the containers.
 * `start`. Start the dockerised environment without building the project and tail the logs of all the containers.
 * `stop`. Stop the dockerised environment.
 * `purge`. Stop the dockerised container and delete all the persistent data (docker volumes).
 * `tail`. Tail the logs of all the containers.
 * `reload_acs`. Build the ACS module, recreate the ACS docker image and restart the ACS container.
 * `build_test`. Build the whole project, recreate the ACS docker image, start the dockerised environment, execute the integration tests and stop 
 the environment.
 * `test`. Execute the integration tests (the environment must be already started).

