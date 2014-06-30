package org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;

public class PdfboxFacadeIntegrationTest {

  @Test
  public void test() {
    String filename = "test.pdf";
    InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
    PdfboxFacade facade = new PdfboxFacade(inputStream, outputStream);
    
    facade.setAuthor("John Smith");
    
    try {
      facade.save();
    } catch (ContentException ex) {
      ex.printStackTrace();
      
      fail();
    }
    
    File tempFile = TempFileProvider.createTempFile("PdfboxFacadeIntegrationTest", ".pdf");
    
    try {
      FileUtils.writeByteArrayToFile(tempFile, outputStream.toByteArray());
      
      long originalFileLength = IOUtils.toByteArray(Thread.currentThread().getContextClassLoader().getResourceAsStream(filename)).length;
      long newFileLength = tempFile.length();
      
      assertTrue(originalFileLength != newFileLength);
    } catch (IOException ex1) {
      ex1.printStackTrace();
      
      fail();
    } finally {
      tempFile.delete();
    }
  }

}
