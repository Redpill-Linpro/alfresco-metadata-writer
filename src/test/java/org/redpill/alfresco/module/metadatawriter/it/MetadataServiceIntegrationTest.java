package org.redpill.alfresco.module.metadatawriter.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.exceptions.CryptographyException;
import org.apache.pdfbox.exceptions.InvalidPasswordException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService.UpdateMetadataException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "classpath:alfresco/application-context.xml", "classpath:test-application-context.xml" })
public class MetadataServiceIntegrationTest extends AbstractMetadataWriterIntegrationTest {

  private static final String TEST_TITLE = "This is a test öäåÖÄÅ";

  private static final String DEFAULT_USERNAME = "testuser";

  private static SiteInfo _site;

  @Autowired
  @Qualifier("metadata-writer.test-service")
  private MetadataService _metadataService;

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
  public void testWriteEmptyPdf() {
    doTestWrite("empty.pdf", null, true, true);
  }
  
  @Test
  public void testWriteEmptyDoc() {
    doTestWrite("empty.doc", null, true, true);
  }
  
  @Test
  public void testWriteEmptyXls() {
    doTestWrite("empty.xls", null, true, true);
  }
  
  @Test
  public void testWriteEmptyPpt() {
    doTestWrite("empty.ppt", null, true, true);
  }
  
  @Test
  public void testWriteEmptyDocx() {
    doTestWrite("empty.docx", null, true, true);
  }
  
  @Test
  public void testWriteEmptyXlsx() {
    doTestWrite("empty.xlsx", null, true, true);
  }
  
  @Test
  public void testWriteEmptyPptx() {
    doTestWrite("empty.pptx", null, true, true);
  }
  
  @Test
  public void testWriteEmptyOdt() {
    doTestWrite("empty.odt", null, true, true);
  }
  
  @Test
  public void testWriteEmptyOds() {
    doTestWrite("empty.ods", null, true, true);
  }
  
  @Test
  public void testWriteEmptyOdp() {
    doTestWrite("empty.odp", null, true, true);
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

  @Test
  public void testWritePdf1() throws ContentIOException, COSVisitorException, IOException, CryptographyException, InvalidPasswordException {
    doTestWritePdf("test.pdf", true, 7990, 8000);
  }

  @Test
  public void testWritePdf2() throws ContentIOException, COSVisitorException, IOException, CryptographyException, InvalidPasswordException {
    doTestWritePdf("secure.pdf", false, 18090, 18100);
  }

  @Test
  public void testWritePdf3() throws ContentIOException, COSVisitorException, IOException, CryptographyException, InvalidPasswordException {
    doTestWritePdf("test_pdfa.pdf", true, 22640, 22660);
  }

  public void doTestWritePdf(String filename, boolean validate, int expectedMin, int expectedMax) throws ContentIOException, IOException, COSVisitorException, CryptographyException, InvalidPasswordException {
    NodeRef document = doTestWrite(filename, null, validate, false);

    ContentReader reader = _contentService.getReader(document, ContentModel.PROP_CONTENT);

    assertBetween(expectedMin,expectedMax, reader.getSize());

    PDDocument pdfDocument = PDDocument.load(reader.getContentInputStream());

    if (pdfDocument.isEncrypted()) {
      return;
    }

    File tempfile = new File(TempFileProvider.getTempDir(), filename);

    pdfDocument.save(tempfile);

    try {
      assertEquals(TEST_TITLE, pdfDocument.getDocumentInformation().getTitle());
      assertEquals(filename, pdfDocument.getDocumentInformation().getCustomMetadataValue("cm:name"));
    } finally {
      pdfDocument.close();
      tempfile.delete();
    }
  }

  private void assertBetween(long expectedMin, long expectedMax, long actual) {
    assertTrue("Expected "+actual+ " to be greater or equal to "+expectedMin, expectedMin<=actual);
    assertTrue("Expected "+actual+ " to be lesser or equal to "+expectedMax, expectedMax>=actual);
  }

  public NodeRef doTestWrite(String filename) {
    return doTestWrite(filename, null, true, false);
  }

  public NodeRef doTestWrite(String filename, String mimetype) {
    return doTestWrite(filename, mimetype, true, false);
  }

  public NodeRef doTestWrite(String filename, String mimetype, boolean validate, boolean expectNull) {
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
        testWriteInTransaction(document);

        return document;
      }
    };

    NodeRef result = _transactionHelper.doInTransaction(callback, false, true);

    if (validate && !expectNull) {
      assertTitle(document, TEST_TITLE);
    } else if (validate && expectNull) {
      assertTitle(document, null);
    }

    return result;
  }

  protected void testWriteInTransaction(NodeRef document) throws UpdateMetadataException {
    _nodeService.setProperty(document, ContentModel.PROP_TITLE, TEST_TITLE);

    _metadataService.write(document);
  }
  
  @Test
  public void testAssertCorrectModifierAfterMdw() {
    
  }

}
