package org.redpill.alfresco.module.metadatawriter.it;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.metadata.MetadataExtracter;
import org.alfresco.repo.content.metadata.MetadataExtracterRegistry;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.MimetypeService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.test.AbstractComponentIT;
import org.springframework.context.ApplicationContext;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public abstract class AbstractMetadataWriterIT extends AbstractComponentIT {


  private MetadataContentFactory _metadataContentFactory;


  private MetadataExtracterRegistry _metadataExtracterRegistry;


  private MimetypeService _mimetypeService;

  ApplicationContext ctx;
  private boolean oldIsRequireNew;

  @Before
  public void setUp() {
    oldIsRequireNew = isRequiresNew();
    setRequiresNew(false);
    ctx = getApplicationContext();
    _metadataContentFactory = (MetadataContentFactory) ctx.getBean("metadata-writer.contentFactory");
    Assert.assertNotNull(_metadataContentFactory);

    _metadataExtracterRegistry = (MetadataExtracterRegistry) ctx.getBean("metadataExtracterRegistry");
    Assert.assertNotNull(_metadataExtracterRegistry);

    _mimetypeService = (MimetypeService) ctx.getBean("MimetypeService");
    Assert.assertNotNull(_mimetypeService);

  }

  @After
  public void tearDown() {
    setRequiresNew(oldIsRequireNew);
  }

  protected void assertTitle(NodeRef document, String expected) {
    assertProperty(document, expected, ContentModel.PROP_TITLE);
  }

  protected void assertProperty(NodeRef document, String expected, QName property) {
    ContentData contentData = (ContentData) nodeService.getProperty(document, ContentModel.PROP_CONTENT);
    String fileName = (String) nodeService.getProperty(document, ContentModel.PROP_NAME);

    String mimetype = null;
    if (contentData != null) {
      mimetype = contentData.getMimetype();
    } else {
      mimetype = _mimetypeService.guessMimetype(fileName);
    }

    MetadataExtracter extracter = _metadataExtracterRegistry.getExtracter(mimetype);

    Map<QName, Serializable> properties = new HashMap<QName, Serializable>();

    ContentReader contentReader = contentService.getReader(document, ContentModel.PROP_CONTENT);

    properties = extracter.extract(contentReader, properties);

    String title = DefaultTypeConverter.INSTANCE.convert(String.class, properties.get(property));

    assertEquals(expected, title);
  }

}
