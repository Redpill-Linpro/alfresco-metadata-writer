package org.redpill.alfresco.module.metadatawriter;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(value=Suite.class)
@SuiteClasses(value={
		org.redpill.alfresco.module.metadatawriter.aspect.AllTests.class,
		org.redpill.alfresco.module.metadatawriter.factories.AllTests.class,
		org.redpill.alfresco.module.metadatawriter.services.AllTests.class,
		org.redpill.alfresco.module.metadatawriter.services.msoffice.AllTests.class})
public class AllTests {

}
     