package org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Calendar;
import java.util.Date;

import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.io.IOUtils;
import org.apache.jempbox.xmp.XMPMetadata;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

public class PdfboxFacade implements ContentFacade {

  private static final Logger LOG = Logger.getLogger(PdfboxFacade.class);

  private InputStream _inputStream;

  private OutputStream _outputStream;

  private InputStream tempInputStream;

  private PDDocument _document;

  private XMPMetadata _xmpMetadata;

  private File tempFile, tempFile2;

  public PdfboxFacade(InputStream inputStream, OutputStream outputStream) {
    if (inputStream == null) {
      throw new ContentIOException("The input stream is null!");
    }

    try {
      _inputStream = inputStream;

      _outputStream = outputStream;

      tempFile = TempFileProvider.createTempFile(_inputStream, "metadatawriter_", ".pdf");
      tempInputStream = new FileInputStream(tempFile);
      _document = PDDocument.load(tempFile);

      // load the XMP metadata, this must be to set the title in both XMP
      // metadata and regular PDF metadata
      loadXmpMetadata();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeMetadata(String field, Serializable value) throws ContentException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + " with value " + value);
    }

    PdfboxMetadata metadata = PdfboxMetadata.find(field);

    metadata.update(field, value, this);
  }

  @Override
  public void save() throws ContentException {
    PdfReader reader = null;

    PdfStamper stamper = null;

    try {
      // Only save changes if the document is not encrypted, otherwise the
      // document will be corrupted.
      boolean copyOriginal = false;

      if (!_document.isEncrypted()) {
        tempFile2 = TempFileProvider.createTempFile(tempInputStream, "metadatawriter2_", ".pdf");

        _document.setAllSecurityToBeRemoved(true);

        saveXmpMetadata();

        _document.save(tempFile2.getAbsolutePath());

        reader = new PdfReader(tempFile2.getAbsolutePath());

        reader.removeUsageRights();

        if (reader.getAcroForm() != null) {
          LOG.warn("Did not write metadata to PDF document since it is contains forms.");

          copyOriginal = true;
        } else {
          stamper = new PdfStamper(reader, _outputStream);

          stamper.setFullCompression();
        }
      } else {
        copyOriginal = true;
        
        LOG.warn("Did not write metadata to PDF document since it is encrypted.");
      }

      if (copyOriginal) {
        // Just copy the input stream to the output stream. With no changes
        // done.
        InputStream tempInputStream = new FileInputStream(tempFile);

        IOUtils.copyLarge(tempInputStream, _outputStream);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      closeQuietly(stamper);
      closeQuietly(reader);
      closeQuietly(_document);
      IOUtils.closeQuietly(_outputStream);
      IOUtils.closeQuietly(_inputStream);
      IOUtils.closeQuietly(tempInputStream);
      closeQuietly(tempFile);
      closeQuietly(tempFile2);
    }
  }

  private void loadXmpMetadata() {
    try {
      _xmpMetadata = _document.getDocumentCatalog().getMetadata().exportXMPMetadata();
    } catch (Throwable ex) {
      return;
    }
  }

  private void saveXmpMetadata() {
    try {
      _document.getDocumentCatalog().getMetadata().importXMPMetadata(_xmpMetadata);
    } catch (Throwable ex) {
      return;
    }
  }

  @Override
  public void abort() throws ContentException {
    IOUtils.closeQuietly(_inputStream);
    IOUtils.closeQuietly(_outputStream);
  }

  public void setAuthor(String author) {
    _document.getDocumentInformation().setAuthor(Normalizer.normalize(author, Form.NFKC));
  }

  /**
   * Sets the title for the PDF.
   * 
   * @param title
   */
  public void setTitle(String title) {
    _document.getDocumentInformation().setTitle(Normalizer.normalize(title, Form.NFKC));

    try {
      if (_xmpMetadata != null) {
        _xmpMetadata.getDublinCoreSchema().setTitle(title);
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void setKeywords(String keywords) {
    _document.getDocumentInformation().setKeywords(Normalizer.normalize(keywords, Form.NFKC));
  }

  public void setCreateDateTime(Date date) {
    Calendar calendar = Calendar.getInstance();

    calendar.setTime(date);

    _document.getDocumentInformation().setCreationDate(calendar);
  }

  public void setCustomMetadata(String field, String value) {
    _document.getDocumentInformation().setCustomMetadataValue(field, Normalizer.normalize(value, Form.NFKC));
  }

  private void closeQuietly(PdfStamper stamper) {
    try {
      stamper.close();
    } catch (Throwable ex) {
      // do nothing here
    }
  }

  private void closeQuietly(PdfReader reader) {
    try {
      reader.close();
    } catch (Throwable ex) {
      // do nothing here
    }
  }

  private void closeQuietly(File tempFile) {
    try {
      tempFile.delete();
    } catch (Throwable ex) {
      // do nothing here
    }
  }

  private void closeQuietly(PDDocument document) {
    try {
      document.close();
    } catch (Throwable ex) {
      // do nothing here
    }
  }

}
