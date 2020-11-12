package org.redpill.alfresco.module.metadatawriter.it;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.util.TempFileProvider;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.junit.*;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService.UpdateMetadataException;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetadataServiceIT extends AbstractMetadataWriterIT {

  private static final String TEST_TITLE = "This is a test öäåÖÄÅ";

  private static String DEFAULT_USERNAME;

  private static SiteInfo site;

  private MetadataService metadataService;

  @Before
  @Override
  public void setUp() {
    DEFAULT_USERNAME = "testuser" + System.currentTimeMillis();
    super.setUp();
    createUser(DEFAULT_USERNAME);
    authenticationComponent.setCurrentUser(DEFAULT_USERNAME);
    site = createSite();
    metadataService = (MetadataService) ctx.getBean("metadata-writer.test-service");
    Assert.assertNotNull(metadataService);
  }

  @After
  @Override
  public void tearDown() {
    if (site != null && nodeService.exists(site.getNodeRef()))
      deleteSite(site);
    authenticationComponent.setCurrentUser(authenticationComponent.getSystemUserName());
    deleteUser(DEFAULT_USERNAME);
    authenticationComponent.clearCurrentSecurityContext();
    super.tearDown();
  }

  @Test
  public void testWriteDoc() throws ContentIOException, IOException, ContentException, UnexpectedPropertySetTypeException, UpdateMetadataException {
    doTestWrite("/test.doc");
  }

  @Test
  public void testWriteDocx() throws UpdateMetadataException {
    doTestWrite("/test.docx");
  }

  @Test
  public void testWriteXls() throws UpdateMetadataException {
    doTestWrite("/test.xls");
  }

  @Test
  public void testWriteXlsx() throws UpdateMetadataException {
    doTestWrite("/test.xlsx");
  }

  @Ignore
  @Test
  public void testWriteOdt() throws UpdateMetadataException {
    doTestWrite("/test.odt", MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT);
  }

  @Ignore
  @Test
  public void testWriteOds() throws UpdateMetadataException {
    doTestWrite("/test.ods", MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET);
  }

  @Ignore
  @Test
  public void testWriteOdp() throws UpdateMetadataException {
    doTestWrite("/test.odp", MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION);
  }

  @Test
  public void testWritePdf1() throws Exception {
    doTestWritePdf("/test.pdf", true, 8000, 8020);
  }

  @Test
  public void testWritePdf2() throws Exception {
    doTestWritePdf("/secure.pdf", false, 18090, 18100);
  }

  @Test
  public void testWritePdf3() throws Exception {
    doTestWritePdf("/test_pdfa.pdf", true, 22660, 22680);
  }

  public void doTestWritePdf(String filename, boolean validate, int expectedMin, int expectedMax) throws Exception {
    NodeRef document = doTestWrite(filename, null, validate, false);
    if (filename.startsWith("/")) {
      filename = filename.substring(1, filename.length());
    }
    ContentReader reader = contentService.getReader(document, ContentModel.PROP_CONTENT);

    assertBetween(expectedMin, expectedMax, reader.getSize());

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
    assertTrue("Expected " + actual + " to be greater or equal to " + expectedMin, expectedMin <= actual);
    assertTrue("Expected " + actual + " to be lesser or equal to " + expectedMax, expectedMax >= actual);
  }

  public NodeRef doTestWrite(String filename) throws UpdateMetadataException {
    return doTestWrite(filename, null, true, false);
  }

  public NodeRef doTestWrite(String filename, String mimetype) throws UpdateMetadataException {
    return doTestWrite(filename, mimetype, true, false);
  }

  public NodeRef doTestWrite(String filename, String mimetype, boolean validate, boolean expectNull) throws UpdateMetadataException {
    setRequiresNew(isRequiresNew());

    final NodeRef document = uploadDocument(site, filename).getNodeRef();

    if (mimetype != null && !mimetype.isBlank()) {
      ContentData contentData = (ContentData) nodeService.getProperty(document, ContentModel.PROP_CONTENT);

      contentData = ContentData.setMimetype(contentData, mimetype);

      nodeService.setProperty(document, ContentModel.PROP_CONTENT, contentData);
    }

    testWriteInTransaction(document);

    if (validate && !expectNull) {
      assertTitle(document, TEST_TITLE);
    } else if (validate && expectNull) {
      assertTitle(document, null);
    }

    return document;
  }

  protected void testWriteInTransaction(NodeRef document) throws UpdateMetadataException {
    nodeService.setProperty(document, ContentModel.PROP_TITLE, TEST_TITLE);

    metadataService.writeSynchronized(document);
  }

  @Test
  public void testAssertCorrectModifierAfterMdw() {

  }

}
