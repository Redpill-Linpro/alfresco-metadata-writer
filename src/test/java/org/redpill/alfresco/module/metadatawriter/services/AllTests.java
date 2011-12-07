package org.redpill.alfresco.module.metadatawriter.services;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;



@RunWith(value=Suite.class)
@SuiteClasses(value={
		MetadataServiceTest.class,
		PropertyValueExtractorTest.class})
public class AllTests {

}
