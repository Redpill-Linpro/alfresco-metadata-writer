package org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;

public class PdfboxFacade implements ContentFacade {

  private final InputStream _inputStream;

  private final OutputStream _outputStream;

  private final PDDocument _document;

  public PdfboxFacade(final InputStream inputStream, final OutputStream outputStream) {
    _inputStream = inputStream;

    _outputStream = outputStream;

    try {
      _document = PDDocument.load(_inputStream);
    } catch (final IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void writeMetadata(final String field, final Serializable value) throws ContentException {
    final PdfboxMetadata metadata = PdfboxMetadata.find(field);

    metadata.update(field, value, this);
  }

  @Override
  public void save() throws ContentException {
    try {
      _document.save(_outputStream);
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      IOUtils.closeQuietly(_inputStream);
      IOUtils.closeQuietly(_outputStream);
    }
  }

  @Override
  public void abort() throws ContentException {
    IOUtils.closeQuietly(_inputStream);
    IOUtils.closeQuietly(_outputStream);
  }

  public void setAuthor(final String author) {
    _document.getDocumentInformation().setAuthor(author);
  }

  public void setTitle(final String title) {
    _document.getDocumentInformation().setTitle(title);
  }

  public void setKeywords(final String keywords) {
    _document.getDocumentInformation().setKeywords(keywords);
  }

  public void setCreateDateTime(final Date date) {
    final Calendar calendar = Calendar.getInstance();

    calendar.setTime(date);

    _document.getDocumentInformation().setCreationDate(calendar);
  }

  public void setCustomMetadata(final String field, final String value) {
    _document.getDocumentInformation().setCustomMetadataValue(field, value);
  }

}
