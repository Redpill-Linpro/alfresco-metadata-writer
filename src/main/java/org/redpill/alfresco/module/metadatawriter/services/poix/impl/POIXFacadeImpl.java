package org.redpill.alfresco.module.metadatawriter.services.poix.impl;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLProperties;
import org.apache.poi.POIXMLProperties.CoreProperties;
import org.apache.poi.POIXMLProperties.CustomProperties;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xslf.XSLFSlideShow;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.poix.POIXFacade;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class POIXFacadeImpl implements POIXFacade {

  private static final Log LOG = LogFactory.getLog(POIXFacadeImpl.class);

  private OutputStream _out;

  private InputStream _in;

  private POIXMLDocument _document;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public POIXFacadeImpl(InputStream in, OutputStream out) throws IOException {
    Assert.notNull(in, "Could not create OpcPackage from null InputStream!");

    _out = out;
    _in = in;

    _document = loadPOIXMLDocument(in);
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  private static POIXMLDocument loadPOIXMLDocument(InputStream in) {
    POIXMLDocument result = null;

    OPCPackage pkg;

    try {
      pkg = OPCPackage.open(in);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    try {
      result = new XWPFDocument(pkg);
    } catch (Exception ex) {
    }

    if (result != null) {
      return result;
    }

    try {
      result = new XSLFSlideShow(pkg);
    } catch (InvalidFormatException ex) {
      throw new org.redpill.alfresco.module.metadatawriter.InvalidFormatException(ex);
    } catch (Exception ex) {
    }

    if (result != null) {
      return result;
    }

    try {
      result = new XSSFWorkbook(pkg);
    } catch (Exception ex) {
    }

    if (result == null) {
      throw new RuntimeException("Couldn't instantiate any document properties from file!");
    }

    return result;
  }

  @Override
  public void setCustomMetadata(String field, String value) throws ContentException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + "=" + value);
    }

    if (_document == null) {
      return;
    }

    POIXMLProperties properties = _document.getProperties();

    CustomProperties customProperties = properties.getCustomProperties();

    if (customProperties.contains(field)) {
      List<CTProperty> underlyingProperties = customProperties.getUnderlyingProperties().getPropertyList();

      for (CTProperty property : underlyingProperties) {
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
  public void setTitle(String title) throws ContentException {
    if (_document == null) {
      return;
    }

    String normalizedTitle = Normalizer.normalize(title, Form.NFKC);

    POIXMLProperties properties = _document.getProperties();

    CoreProperties coreProperties = properties.getCoreProperties();

    coreProperties.setTitle(normalizedTitle);
  }

  @Override
  public void setAuthor(String author) throws ContentException {
    _document.getProperties().getCoreProperties().setCreator(Normalizer.normalize(author, Form.NFKC));
  }

  @Override
  public void setKeywords(String keywords) throws ContentException {
    _document.getProperties().getCoreProperties().setKeywords(Normalizer.normalize(keywords, Form.NFKC));
  }

  @Override
  public void setCreateDateTime(Date dateTime) throws ContentException {
    // this is the proper format for this field
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    String date = formatter.format(dateTime);

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
    LOG.trace("Closing streams");
    IOUtils.closeQuietly(_out);
    IOUtils.closeQuietly(_in);
  }

}
