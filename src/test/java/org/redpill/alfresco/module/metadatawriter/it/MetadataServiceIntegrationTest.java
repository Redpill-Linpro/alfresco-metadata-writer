package org.redpill.alfresco.module.metadatawriter.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.metadata.MetadataExtracter;
import org.alfresco.repo.content.metadata.MetadataExtracterRegistry;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService.UpdateMetadataException;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "classpath:alfresco/application-context.xml", "classpath:test-application-context.xml" })
public class MetadataServiceIntegrationTest extends AbstractRepoIntegrationTest {

  private static final String TEST_TITLE = "This is a test öäåÖÄÅ";

  private static final String DEFAULT_USERNAME = "testuser";

  private static SiteInfo _site;

  @Autowired
  @Qualifier("metadata-writer.test-service")
  private MetadataService _metadataService;

  @Autowired
  private MetadataContentFactory _metadataContentFactory;

  @Autowired
  private MetadataExtracterRegistry _metadataExtracterRegistry;

  @Override
  public void beforeClassSetup() {
    super.beforeClassSetup();

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
  public void testWritePdf() throws ContentIOException, IOException, COSVisitorException {
    NodeRef document = doTestWrite("test.pdf");

    ContentReader reader = _contentService.getReader(document, ContentModel.PROP_CONTENT);

    PDDocument pdfDocument = PDDocument.load(reader.getContentInputStream());

    pdfDocument.save("/tmp/test.pdf");

    try {
      assertEquals(TEST_TITLE, pdfDocument.getDocumentInformation().getTitle());
      assertEquals("test.pdf", pdfDocument.getDocumentInformation().getCustomMetadataValue("cm:name"));
    } finally {
      pdfDocument.close();
    }
  }

  @Test
  public void testWritePdfa() throws ContentIOException, IOException, COSVisitorException {
    NodeRef document = doTestWrite("test_pdfa.pdf");

    ContentReader reader = _contentService.getReader(document, ContentModel.PROP_CONTENT);

    PDDocument pdfDocument = PDDocument.load(reader.getContentInputStream());

    pdfDocument.save("/tmp/test_pdfa.pdf");

    try {
      assertEquals(TEST_TITLE, pdfDocument.getDocumentInformation().getTitle());
      assertEquals("test_pdfa.pdf", pdfDocument.getDocumentInformation().getCustomMetadataValue("cm:name"));
    } finally {
      pdfDocument.close();
    }
  }

  @Test
  public void testWriteDoc() throws ContentIOException, IOException, ContentException, UnexpectedPropertySetTypeException {
    doTestWrite("test.doc");
  }

  @Test
  public void testWriteDocx() {
    doTestWrite("test.docx");
  }

  @Test
  public void testWriteXls() {
    doTestWrite("test.xls");
  }

  @Test
  public void testWriteXlsx() {
    doTestWrite("test.xlsx");
  }

  @Test
  public void testWriteOdt() {
    doTestWrite("test.odt", MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT);
  }

  @Test
  public void testWriteOds() {
    doTestWrite("test.ods", MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET);
  }

  @Test
  public void testWriteOdp() {
    doTestWrite("test.odp", MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION);
  }

  public NodeRef doTestWrite(String filename) {
    return doTestWrite(filename, null);
  }

  public NodeRef doTestWrite(String filename, String mimetype) {
    setRequiresNew(true);

    final NodeRef document = uploadDocument(_site, filename).getNodeRef();

    if (StringUtils.isNotBlank(mimetype)) {
      ContentData contentData = (ContentData) _nodeService.getProperty(document, ContentModel.PROP_CONTENT);

      contentData = ContentData.setMimetype(contentData, mimetype);

      _nodeService.setProperty(document, ContentModel.PROP_CONTENT, contentData);
    }

    RetryingTransactionCallback<NodeRef> callback = new RetryingTransactionCallback<NodeRef>() {

      @Override
      public NodeRef execute() throws Throwable {
        try {
          testWriteInTransaction(document);
        } catch (Exception ex) {
          fail();
          throw ex;
        }

        return document;
      }
    };

    NodeRef result = _transactionHelper.doInTransaction(callback, false, true);
    
    assertTitle(document);

    return result;
  }

  protected void testWriteInTransaction(NodeRef document) throws UpdateMetadataException {
    _nodeService.setProperty(document, ContentModel.PROP_TITLE, TEST_TITLE);

    _metadataService.write(document);
  }

  private void assertTitle(NodeRef document) {
    ContentData contentData = (ContentData) _nodeService.getProperty(document, ContentModel.PROP_CONTENT);

    String mimetype = contentData.getMimetype();

    MetadataExtracter extracter = _metadataExtracterRegistry.getExtracter(mimetype);

    Map<QName, Serializable> properties = new HashMap<QName, Serializable>();

    ContentReader contentReader = _contentService.getReader(document, ContentModel.PROP_CONTENT);

    properties = extracter.extract(contentReader, properties);

    String title = DefaultTypeConverter.INSTANCE.convert(String.class, properties.get(ContentModel.PROP_TITLE));

    System.out.println("KALLE: " + title);

    assertEquals(TEST_TITLE, title);
  }

}
