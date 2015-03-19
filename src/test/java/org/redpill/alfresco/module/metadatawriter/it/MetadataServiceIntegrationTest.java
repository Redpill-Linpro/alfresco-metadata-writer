package org.redpill.alfresco.module.metadatawriter.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;

import javax.annotation.Resource;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.site.SiteInfo;
import org.apache.pdfbox.exceptions.COSVisitorException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService.UpdateMetadataException;
import org.redpill.alfresco.test.AbstractRepoIntegrationTest;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration({ "classpath:alfresco/application-context.xml", "classpath:test-application-context.xml" })
public class MetadataServiceIntegrationTest extends AbstractRepoIntegrationTest {

  private static final String DEFAULT_USERNAME = "testuser";

  private SiteInfo _site;

  @Resource(name = "metadata-writer.test-service")
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
  public void testWritePdf() throws ContentIOException, IOException, COSVisitorException {
    NodeRef document = doTestWrite("test.pdf");

    ContentReader reader = _contentService.getReader(document, ContentModel.PROP_CONTENT);

    PDDocument pdfDocument = PDDocument.load(reader.getContentInputStream());

    pdfDocument.save("/tmp/test.pdf");

    try {
      assertEquals("This is a test öäåÖÄÅ", pdfDocument.getDocumentInformation().getTitle());
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
      assertEquals("This is a test öäåÖÄÅ", pdfDocument.getDocumentInformation().getTitle());
      assertEquals("test_pdfa.pdf", pdfDocument.getDocumentInformation().getCustomMetadataValue("cm:name"));
    } finally {
      pdfDocument.close();
    }
  }

  @Test
  public void testWriteDoc() throws ContentIOException, IOException, ContentException, UnexpectedPropertySetTypeException {
    doTestWrite("test.doc");
    
    // NodeRef document = doTestWrite("test.doc");
    //
    // ContentReader reader = _contentService.getReader(document,
    // ContentModel.PROP_CONTENT);
    //
    // POIFSFileSystem fileSystem = new
    // POIFSFileSystem(reader.getContentInputStream());
    //
    // PropertySet propertySet =
    // POIFSFacadeImpl.createPropertySet(SummaryInformation.DEFAULT_STREAM_NAME,
    // fileSystem);
    //
    // SummaryInformation summaryInformation = new
    // SummaryInformation(propertySet);
    // DocumentSummaryInformation documentSummaryInformation =
    // POIFSFacadeImpl.getDocumentSummaryInformation(fileSystem);
    //
    // assertEquals("This is a test öäåÖÄÅ", summaryInformation.getTitle());
    // assertEquals("test.pdf",
    // documentSummaryInformation.getCustomProperties().get("cm:name"));
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

  public NodeRef doTestWrite(String filename) {
    FileInfo file = uploadDocument(_site, filename);

    final NodeRef document = file.getNodeRef();

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

    return _transactionHelper.doInTransaction(callback, false, false);
  }

  protected NodeRef testWriteInTransaction(NodeRef document) throws UpdateMetadataException {
    _nodeService.setProperty(document, ContentModel.PROP_TITLE, "This is a test öäåÖÄÅ");

    _metadataService.write(document);

    return document;
  }

}
