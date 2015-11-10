package org.redpill.alfresco.module.metadatawriter.services.odf.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.odftoolkit.odfdom.dom.OdfMetaDom;
import org.odftoolkit.simple.Document;
import org.odftoolkit.simple.meta.Meta;
import org.redpill.alfresco.module.metadatawriter.services.odf.OdfFacade;
import org.springframework.util.Assert;

public class OdfFacadeImpl implements OdfFacade {

  private static final Log LOG = LogFactory.getLog(OdfFacadeImpl.class);

  private OutputStream _out;

  private InputStream _in;

  private Document _document;

  private Meta _metadata;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public OdfFacadeImpl(InputStream in, OutputStream out) {
    Assert.notNull(in, "Could not load document from null InputStream!");

    _out = out;
    _in = in;

    try {
      _document = Document.loadDocument(in);

      OdfMetaDom metaDom = _document.getMetaDom();

      _metadata = new Meta(metaDom);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  @Override
  public void setCustomMetadata(String field, String value) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + "=" + value);
    }

    String normalValue = Normalizer.normalize(value, Form.NFKC);

    _metadata.setUserDefinedDataValue(field, normalValue);
  }

  @Override
  public void setTitle(String title) {
    String normalTitle = Normalizer.normalize(title, Form.NFKC);

    _metadata.setTitle(normalTitle);
  }

  @Override
  public void setAuthor(String author) {
    String normalCreator = Normalizer.normalize(author, Form.NFKC);

    _metadata.setCreator(normalCreator);
  }

  @Override
  public void setKeywords(String keywords) {
    String normalKeywords = Normalizer.normalize(keywords, Form.NFKC);

    _metadata.addKeyword(normalKeywords);
  }

  @Override
  public void setCreateDateTime(Date dateTime) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTime(dateTime);

    _metadata.setCreationDate(calendar);
  }

  /**
   * Writes the updated properties to the out stream. Closes both input and
   * output when done.
   */
  @Override
  public void writeProperties() {
    try {
      _document.save(_out);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    } finally {
      closeStreams();
    }
  }

  @Override
  public void close() throws IOException {
    closeStreams();
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------

  private void closeStreams() {
    LOG.trace("Closing streams");
    IOUtils.closeQuietly(_out);
    IOUtils.closeQuietly(_in);
  }

}
