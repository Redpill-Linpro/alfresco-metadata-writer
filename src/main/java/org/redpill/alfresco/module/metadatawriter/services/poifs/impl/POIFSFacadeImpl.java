package org.redpill.alfresco.module.metadatawriter.services.poifs.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;
import org.apache.poi.hpsf.CustomProperties;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.apache.poi.hpsf.WritingNotSupportedException;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl.PdfboxFacade;
import org.redpill.alfresco.module.metadatawriter.services.poifs.POIFSFacade;

public class POIFSFacadeImpl implements POIFSFacade {

  private static final Logger LOG = Logger.getLogger(POIFSFacadeImpl.class);
  private POIFSFileSystem fileSystem;
  private OutputStream out;
  private InputStream in;
  private DocumentSummaryInformation dsi;
  private SummaryInformation si;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public POIFSFacadeImpl(InputStream in, OutputStream out) throws IOException {

    if (in == null) {
      throw new IOException("Could not create POIFSFileSystem from null InputStream!");
    }

    this.out = out;
    this.in = in;
    this.fileSystem = new POIFSFileSystem(in);

  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  public void setCustomMetadata(String field, String value) throws ContentException {

    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + "=" + value);
    }

    CustomProperties customProperties = getCustomProperties();

    customProperties.put(field, Normalizer.normalize(value, Form.NFKC));

    getDocumentSummaryInformation().setCustomProperties(customProperties);

    saveDocumentSummaryInformation();
  }

  public void setTitle(String title) throws ContentException {
    getSummaryInformation().setTitle(Normalizer.normalize(title, Form.NFKC));
    saveSummaryInformation();
  }

  public void setAuthor(String author) throws ContentException {
    getSummaryInformation().setAuthor(Normalizer.normalize(author, Form.NFKC));
    saveSummaryInformation();
  }

  public void setKeywords(String keywords) throws ContentException {
    getSummaryInformation().setKeywords(Normalizer.normalize(keywords, Form.NFKC));
    saveSummaryInformation();
  }

  public void setCreateDateTime(Date dateTime) throws ContentException {
    getSummaryInformation().setCreateDateTime(dateTime);
    saveSummaryInformation();
  }

  /**
   * Writes the updated properties to the out stream. Closes both input and
   * output when done.
   */
  public void writeProperties() throws IOException {
    try {
      fileSystem.writeFilesystem(out);
    } finally {
      close();
    }
  }

  public void close() throws IOException {
    LOG.trace("Closing streams");
    IOUtils.closeQuietly(out);
    IOUtils.closeQuietly(in);
  }

  /*
   * 
   * private static String describe( CustomProperties p) { StringBuilder sb =
   * new StringBuilder(); for ( Object key : p.keySet()) { sb.append("\n\t" +
   * key + "=" + p.get(key)); }
   * 
   * return sb.toString();
   * 
   * }
   */

  private CustomProperties getCustomProperties() throws ContentException {
    CustomProperties customProperties = getDocumentSummaryInformation().getCustomProperties();
    if (customProperties == null) {
      customProperties = new CustomProperties();
    }
    return customProperties;
  }

  private void saveSummaryInformation() throws ContentException {
    try {
      getSummaryInformation().write(getFileSystem().getRoot(), SummaryInformation.DEFAULT_STREAM_NAME);
    } catch (IOException e) {
      throw new ContentFacade.ContentException("Could not write Summary Information", e);
    } catch (WritingNotSupportedException e) {
      throw new ContentFacade.ContentException("Could not write Summary Information", e);
    }
  }

  private void saveDocumentSummaryInformation() throws ContentException {
    try {
      getDocumentSummaryInformation().write(getFileSystem().getRoot(), DocumentSummaryInformation.DEFAULT_STREAM_NAME);
    } catch (IOException e) {
      throw new ContentFacade.ContentException("Could not write Document Summary Information", e);
    } catch (WritingNotSupportedException e) {
      throw new ContentFacade.ContentException("Could not write Document Summary Information", e);
    }
  }

  private POIFSFileSystem getFileSystem() throws IOException {
    return fileSystem;
  }

  private SummaryInformation getSummaryInformation() throws ContentException {
    if (null == si) {
      try {
        PropertySet ps = createPropertySet(SummaryInformation.DEFAULT_STREAM_NAME);
        si = new SummaryInformation(ps);
      } catch (FileNotFoundException fnf) {
        LOG.debug("Summary information does not exist in file, creating new!");
        si = PropertySetFactory.newSummaryInformation();
      } catch (UnexpectedPropertySetTypeException e) {
        throw new ContentException("Summary information property set has invalid type", e);
      }
    }

    return si;
  }

  public static DocumentSummaryInformation getDocumentSummaryInformation(POIFSFileSystem fileSystem) throws ContentException {
    try {
      PropertySet ps = createPropertySet(DocumentSummaryInformation.DEFAULT_STREAM_NAME, fileSystem);
      return new DocumentSummaryInformation(ps);
    } catch (FileNotFoundException fnf) {
      LOG.debug("Document summary information does not exist in file, createing new!");

      return PropertySetFactory.newDocumentSummaryInformation();
    } catch (UnexpectedPropertySetTypeException e) {
      throw new ContentException("Document summary information property set has invalid type", e);
    }
  }

  private DocumentSummaryInformation getDocumentSummaryInformation() throws ContentException {
    if (dsi == null) {
      try {
        dsi = getDocumentSummaryInformation(getFileSystem());
      } catch (IOException ex) {
        throw new ContentException(ex.getMessage(), ex);
      }
    }

    return dsi;
  }

  public static PropertySet createPropertySet(String streamName, POIFSFileSystem fileSystem) throws ContentException, FileNotFoundException {
    try {
      DirectoryEntry dir = fileSystem.getRoot();
      DocumentEntry dsiEntry = (DocumentEntry) dir.getEntry(streamName);
      DocumentInputStream dis = new DocumentInputStream(dsiEntry);
      PropertySet ps = new PropertySet(dis);
      dis.close();
      return ps;
    } catch (NoPropertySetStreamException e) {
      throw new ContentException("Format error in stream " + streamName, e);
    } catch (MarkUnsupportedException e) {
      throw new ContentException("Could not create PropertySet for stream " + streamName, e);
    } catch (UnsupportedEncodingException e) {
      throw new ContentException("Unsupported encoding in stream: " + streamName, e);
    } catch (IOException e) {
      throw new ContentException("Could not read stream " + streamName, e);
    }
  }

  private PropertySet createPropertySet(String streamName) throws ContentException, FileNotFoundException {
    POIFSFileSystem fileSystem;

    try {
      fileSystem = getFileSystem();
    } catch (IOException ex) {
      throw new ContentException("Could not read stream " + streamName, ex);
    }

    return POIFSFacadeImpl.createPropertySet(streamName, fileSystem);
  }

}
