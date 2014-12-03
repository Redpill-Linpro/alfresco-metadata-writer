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
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;

public class PdfboxFacade implements ContentFacade {

  private static final Logger LOG = Logger.getLogger(PdfboxFacade.class);

  private final InputStream _inputStream;

  private final OutputStream _outputStream;

  private final InputStream tempInputStream;

  private PDDocument _document;

  File tempFile, tempFile2;

  public PdfboxFacade(final InputStream inputStream, final OutputStream outputStream) {
    if (inputStream == null) {
      throw new ContentIOException("The input stream is null!");
    }

    try {
      _inputStream = inputStream;

      _outputStream = outputStream;

      tempFile = TempFileProvider.createTempFile(_inputStream, "metadatawriter_", ".pdf");
      tempInputStream = new FileInputStream(tempFile);
      _document = PDDocument.load(tempFile);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void writeMetadata(final String field, final Serializable value) throws ContentException {
    final PdfboxMetadata metadata = PdfboxMetadata.find(field);

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
        LOG.warn("Did not write metadata to PDF document since it is encrypted.");
      }
      if (copyOriginal) {
        // Just copy the input stream to the output stream. With no changes
        // done.
        InputStream tempInputStream = new FileInputStream(tempFile);
        IOUtils.copyLarge(tempInputStream, _outputStream);
      }
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      closeQuietly(stamper);

      closeQuietly(reader);

      try {
        if (_document != null)
          _document.close();
      } catch (IOException e) {

      }

      IOUtils.closeQuietly(_outputStream);

      IOUtils.closeQuietly(_inputStream);

      IOUtils.closeQuietly(tempInputStream);

      closeQuietly(tempFile);
      closeQuietly(tempFile2);
    }
  }

  @Override
  public void abort() throws ContentException {
    IOUtils.closeQuietly(_inputStream);
    IOUtils.closeQuietly(_outputStream);
  }

  public void setAuthor(final String author) {
    _document.getDocumentInformation().setAuthor(Normalizer.normalize(author, Form.NFKC));
  }

  public void setTitle(final String title) {
    _document.getDocumentInformation().setTitle(Normalizer.normalize(title, Form.NFKC));
  }

  public void setKeywords(final String keywords) {
    _document.getDocumentInformation().setKeywords(Normalizer.normalize(keywords, Form.NFKC));
  }

  public void setCreateDateTime(final Date date) {
    final Calendar calendar = Calendar.getInstance();

    calendar.setTime(date);

    _document.getDocumentInformation().setCreationDate(calendar);
  }

  public void setCustomMetadata(final String field, final String value) {
    _document.getDocumentInformation().setCustomMetadataValue(field, Normalizer.normalize(value, Form.NFKC));
  }

  private void closeQuietly(PdfStamper stamper) {
    try {
      stamper.close();
    } catch (Exception ex) {
      // do nothing here
    }
  }

  private void closeQuietly(PdfReader reader) {
    try {
      reader.close();
    } catch (Exception ex) {
      // do nothing here
    }
  }

  private void closeQuietly(File tempFile) {
    try {
      tempFile.delete();
    } catch (Exception ex) {
      // do nothing here
    }
  }

}
