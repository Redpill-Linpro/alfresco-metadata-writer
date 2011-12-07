package org.redpill.alfresco.module.metadatawriter.factories;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;


@RunWith(value=Suite.class)
@SuiteClasses(value={
		MetadataContentFactoryTest.class, 
		MetadataServiceRegistryTest.class})
public class AllTests {

}
