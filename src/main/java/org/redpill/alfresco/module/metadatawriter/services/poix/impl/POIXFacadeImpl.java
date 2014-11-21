package org.redpill.alfresco.module.metadatawriter.services.poix.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLProperties.CustomProperties;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xslf.XSLFSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.poix.POIXFacade;
import org.springframework.util.Assert;

public class POIXFacadeImpl implements POIXFacade {

  private static final Log LOG = LogFactory.getLog(POIXFacadeImpl.class);

  private final OutputStream _out;

  private final InputStream _in;

  private final POIXMLDocument _document;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public POIXFacadeImpl(final InputStream in, final OutputStream out) throws IOException {
    Assert.notNull(in, "Could not create OpcPackage from null InputStream!");

    _out = out;
    _in = in;

    _document = loadPOIXMLDocument(in);
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  private static POIXMLDocument loadPOIXMLDocument(final InputStream in) {
    POIXMLDocument result = null;

    OPCPackage pkg;
    try {
      pkg = OPCPackage.open(in);
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }

    try {
      result = new XWPFDocument(pkg);
    } catch (final Exception ex) {
    }

    if (result != null) {
      return result;
    }

    try {
      result = new XSLFSlideShow(pkg);
    } catch (final Exception ex) {
    }

    if (result != null) {
      return result;
    }

    try {
      result = new XSSFWorkbook(pkg);
    } catch (final Exception ex) {
    }

    return result;
  }

  @Override
  public void setCustomMetadata(final String field, final String value) throws ContentException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + "=" + value);
    }

    final CustomProperties customProperties = _document.getProperties().getCustomProperties();

    if (customProperties.contains(field)) {
      final List<CTProperty> properties = customProperties.getUnderlyingProperties().getPropertyList();

      for (final CTProperty property : properties) {
        if (!property.getName().equalsIgnoreCase(field)) {
          continue;
        }

        property.setLpwstr(Normalizer.normalize(value, Form.NFKC));
      }
    } else {
      customProperties.addProperty(field, Normalizer.normalize(value, Form.NFKC));
    }
  }

  @Override
  public void setTitle( String title) throws ContentException {
    _document.getProperties().getCoreProperties().setTitle(Normalizer.normalize(title, Form.NFKC));
  }

  @Override
  public void setAuthor( String author) throws ContentException {
    _document.getProperties().getCoreProperties().setCreator(Normalizer.normalize(author, Form.NFKC));
  }

  @Override
  public void setKeywords( String keywords) throws ContentException {
    _document.getProperties().getCoreProperties().setKeywords(Normalizer.normalize(keywords, Form.NFKC));
  }

  @Override
  public void setCreateDateTime(final Date dateTime) throws ContentException {
    // this is the proper format for this field
    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    final String date = formatter.format(dateTime);

    _document.getProperties().getCoreProperties().setCreated(date);
  }

  /**
   * Writes the updated properties to the out stream. Closes both input and
   * output when done.
   */
  @Override
  public void writeProperties() throws IOException {
    try {
      _document.write(_out);
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

  private void closeStreams() throws IOException {
    IOUtils.closeQuietly(_out);
    IOUtils.closeQuietly(_in);
  }

}
