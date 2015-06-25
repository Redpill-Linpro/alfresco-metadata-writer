package org.redpill.alfresco.module.metadatawriter.it;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.namespace.QName;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.model.MetadataWriterModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "classpath:alfresco/application-context.xml", "classpath:test-application-context.xml" })
public class ExportMetadataAspectIntegrationTest extends AbstractMetadataWriterIntegrationTest {

  private static final String DEFAULT_USERNAME = "testuser";

  private static SiteInfo _site;

  @Autowired
  @Qualifier("policyBehaviourFilter")
  private BehaviourFilter _behaviourFilter;

  @Override
  public void beforeClassSetup() {
    super.beforeClassSetup();

    setRequiresNew(false);

    createUser(DEFAULT_USERNAME);

    _authenticationComponent.setCurrentUser(DEFAULT_USERNAME);

    _site = createSite();
  }

  @Override
  public void afterClassSetup() {
    deleteSite(_site);

    _authenticationComponent.setCurrentUser(_authenticationComponent.getSystemUserName());

    deleteUser(DEFAULT_USERNAME);

    _authenticationComponent.clearCurrentSecurityContext();

    super.afterClassSetup();
  }

  @Test
  public void testAspect() throws InterruptedException {
    final String title = "This is a title";

    final NodeRef document = uploadDocument(_site, "test.doc").getNodeRef();

    _nodeService.addAspect(document, ContentModel.ASPECT_VERSIONABLE, null);
    _nodeService.setProperty(document, ContentModel.PROP_AUTO_VERSION_PROPS, true);

    String version = (String) _nodeService.getProperty(document, ContentModel.PROP_VERSION_LABEL);

    assertEquals("1.0", version);

    final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
    properties.put(MetadataWriterModel.PROP_METADATA_SERVICE_NAME, "foobar.metadata-writer.service");

    _nodeService.addAspect(document, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, properties);
    _nodeService.setProperty(document, ContentModel.PROP_TITLE, title);

    version = (String) _nodeService.getProperty(document, ContentModel.PROP_VERSION_LABEL);

    assertTitle(document, title);

    assertEquals("1.2", version);

    Thread.sleep(1000);
  }

}
