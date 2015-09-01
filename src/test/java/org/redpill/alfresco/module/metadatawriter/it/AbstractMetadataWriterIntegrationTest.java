package org.redpill.alfresco.module.metadatawriter.it;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.metadata.MetadataExtracter;
import org.alfresco.repo.content.metadata.MetadataExtracterRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractMetadataWriterIntegrationTest extends AbstractRepoIntegrationTest {

  @Autowired
  private MetadataContentFactory _metadataContentFactory;

  @Autowired
  private MetadataExtracterRegistry _metadataExtracterRegistry;

  protected void assertTitle(NodeRef document, String expected) {
    assertProperty(document, expected, ContentModel.PROP_TITLE);
  }

  protected void assertProperty(NodeRef document, String expected, QName property) {
    ContentData contentData = (ContentData) _nodeService.getProperty(document, ContentModel.PROP_CONTENT);

    String mimetype = contentData.getMimetype();

    MetadataExtracter extracter = _metadataExtracterRegistry.getExtracter(mimetype);

    Map<QName, Serializable> properties = new HashMap<QName, Serializable>();

    ContentReader contentReader = _contentService.getReader(document, ContentModel.PROP_CONTENT);

    properties = extracter.extract(contentReader, properties);

    String title = DefaultTypeConverter.INSTANCE.convert(String.class, properties.get(property));

    assertEquals(expected, title);
  }

}
