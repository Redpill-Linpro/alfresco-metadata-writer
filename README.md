##Using the metadata writer ##

* Include the module in your project
* Define a bean in a -context.xml file:
```xml
<bean id="my.metadata.service.id" class="org.redpill.alfresco.module.metadatawriter.services.impl.MetadataServiceImpl" parent="metadata-writer.abstract.service">
      <property name="serviceName" value="my.metadata.service" />
      <property name="converters">
        <list>
          <ref bean="hav.metadata-writer.dateConverter" />
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
  on the document.

* In the 'serviceName' property, you define the name of your custom metadata service.
 
* In the 'converters' property, you define a list of converter that can be used to format or convert property values.
 
* To create custom converters you need two things, a class implementing 'ValueConverter' and 
 a bean-definition for that class. Then you can register your custom converter in a metadataService bean.
 
* To indicate that a documents metadata should be written, add the 'mdw:serviceName' and set the value
 to be the 'serviceName' of your custom metadataService. In the example this would be 'my.metadata.service'.