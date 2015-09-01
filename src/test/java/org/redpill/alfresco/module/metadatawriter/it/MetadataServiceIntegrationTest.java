package org.redpill.alfresco.module.metadatawriter.it;

import static org.junit.Assert.assertEquals;

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
    doTestWritePdf("test.pdf", true, 7996);
  }

  @Test
  public void testWritePdf2() throws ContentIOException, COSVisitorException, IOException, CryptographyException, InvalidPasswordException {
    doTestWritePdf("secure.pdf", false, 18097);
  }

  @Test
  public void testWritePdf3() throws ContentIOException, COSVisitorException, IOException, CryptographyException, InvalidPasswordException {
    doTestWritePdf("test_pdfa.pdf", true, 22644);
  }

  public void doTestWritePdf(String filename, boolean validate, int expectedSize) throws ContentIOException, IOException, COSVisitorException, CryptographyException, InvalidPasswordException {
    NodeRef document = doTestWrite(filename, null, validate);

    ContentReader reader = _contentService.getReader(document, ContentModel.PROP_CONTENT);

    assertEquals(expectedSize, reader.getSize());

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

  public NodeRef doTestWrite(String filename) {
    return doTestWrite(filename, null, true);
  }

  public NodeRef doTestWrite(String filename, String mimetype) {
    return doTestWrite(filename, mimetype, true);
  }

  public NodeRef doTestWrite(String filename, String mimetype, boolean validate) {
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

    if (validate) {
      assertTitle(document, TEST_TITLE);
    }

    return result;
  }

  protected void testWriteInTransaction(NodeRef document) throws UpdateMetadataException {
    _nodeService.setProperty(document, ContentModel.PROP_TITLE, TEST_TITLE);

    _metadataService.write(document);
  }

}
