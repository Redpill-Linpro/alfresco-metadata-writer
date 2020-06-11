package org.redpill.alfresco.module.metadatawriter.it;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.namespace.QName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.model.MetadataWriterModel;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ExportMetadataAspectIT extends AbstractMetadataWriterIT {

  private static final String DEFAULT_USERNAME = "testuser" + System.currentTimeMillis();

  private static SiteInfo _site;


  @Before
  @Override
  public void setUp() {
    super.setUp();
    //createUser(DEFAULT_USERNAME);
    //authenticationComponent.setCurrentUser(DEFAULT_USERNAME);
    _site = createSite();
  }

  @After
  @Override
  public void tearDown() {
    deleteSite(_site);
    //authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());
    //deleteUser(DEFAULT_USERNAME);
    //authenticationComponent.clearCurrentSecurityContext();
    super.tearDown();
  }

  @Test
  public void testAspect() throws InterruptedException {
    final String title = "This is a title";

    final NodeRef document = uploadDocument(_site, "/test.doc").getNodeRef();

    nodeService.addAspect(document, ContentModel.ASPECT_VERSIONABLE, null);
    nodeService.setProperty(document, ContentModel.PROP_AUTO_VERSION_PROPS, true);

    String version = (String) nodeService.getProperty(document, ContentModel.PROP_VERSION_LABEL);

    assertEquals("1.0", version);

    final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
    properties.put(MetadataWriterModel.PROP_METADATA_SERVICE_NAME, "foobar.metadata-writer.service");

    nodeService.addAspect(document, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, properties);
    nodeService.setProperty(document, ContentModel.PROP_TITLE, title);

    version = (String) nodeService.getProperty(document, ContentModel.PROP_VERSION_LABEL);

    assertTitle(document, title);

    assertEquals("1.2", version);

    Thread.sleep(1000);
  }

}
