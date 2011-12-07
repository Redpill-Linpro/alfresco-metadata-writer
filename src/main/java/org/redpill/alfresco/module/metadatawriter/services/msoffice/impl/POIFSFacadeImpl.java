package org.redpill.alfresco.module.metadatawriter.services.msoffice.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.redpill.alfresco.module.metadatawriter.services.msoffice.POIFSFacade;

public class POIFSFacadeImpl implements POIFSFacade {

  private static final Log logger = LogFactory.getLog(POIFSFacadeImpl.class);

  private final POIFSFileSystem fileSystem;
  private final OutputStream out;
  private final InputStream in;
  private DocumentSummaryInformation dsi;
  private SummaryInformation si;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public POIFSFacadeImpl(final InputStream in, final OutputStream out) throws IOException {

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

  public void setCustomMetadata(final String field, final String value) throws ContentException {

    if (logger.isDebugEnabled()) {
      logger.debug("Exporting metadata " + field + "=" + value);
    }

    final CustomProperties customProperties = getCustomProperties();

    // if(logger.isDebugEnabled()) {
    // logger.debug("CustomProperties before export: " +
    // describe(customProperties));
    // }

    customProperties.put(field, value);

    getDocumentSummaryInformation().setCustomProperties(customProperties);

    saveDocumentSummaryInformation();

    // if(logger.isDebugEnabled()) {
    // logger.debug("CustomProperties after export: " +
    // describe(getCustomProperties()));
    // }

  }

  public void setTitle(final String title) throws ContentException {
    getSummaryInformation().setTitle(title);
    saveSummaryInformation();
  }

  public void setAuthor(final String author) throws ContentException {
    getSummaryInformation().setAuthor(author);
    saveSummaryInformation();
  }

  public void setKeywords(final String keywords) throws ContentException {
    getSummaryInformation().setKeywords(keywords);
    saveSummaryInformation();
  }

  public void setCreateDateTime(final Date dateTime) throws ContentException {
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
      closeStreams();
    }
  }

  public void close() throws IOException {
    closeStreams();
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------

  private void closeStreams() throws IOException {
    try {
      out.close();
    } finally {
      in.close();
    }
  }

  /*
   * 
   * private static String describe(final CustomProperties p) { final
   * StringBuilder sb = new StringBuilder(); for (final Object key : p.keySet())
   * { sb.append("\n\t" + key + "=" + p.get(key)); }
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
        final PropertySet ps = createPropertySet(SummaryInformation.DEFAULT_STREAM_NAME);
        si = new SummaryInformation(ps);
      } catch (FileNotFoundException fnf) {
        logger.debug("Summary information does not exist in file, creating new!");
        si = PropertySetFactory.newSummaryInformation();
      } catch (UnexpectedPropertySetTypeException e) {
        throw new ContentException("Summary information property set has invalid type", e);
      }
    }

    return si;
  }

  private DocumentSummaryInformation getDocumentSummaryInformation() throws ContentFacade.ContentException {
    if (null == dsi) {
      try {
        final PropertySet ps = createPropertySet(DocumentSummaryInformation.DEFAULT_STREAM_NAME);
        dsi = new DocumentSummaryInformation(ps);
      } catch (FileNotFoundException fnf) {
        logger.debug("Document summary information does not exist in file, createing new!");
        dsi = PropertySetFactory.newDocumentSummaryInformation();
      } catch (UnexpectedPropertySetTypeException e) {
        throw new ContentException("Document summary information property set has invalid type", e);
      }
    }

    return dsi;

  }

  private PropertySet createPropertySet(final String streamName) throws ContentFacade.ContentException, FileNotFoundException {

    try {
      final DirectoryEntry dir = getFileSystem().getRoot();
      final DocumentEntry dsiEntry = (DocumentEntry) dir.getEntry(streamName);
      final DocumentInputStream dis = new DocumentInputStream(dsiEntry);
      final PropertySet ps = new PropertySet(dis);
      dis.close();
      return ps;
    } catch (NoPropertySetStreamException e) {
      throw new ContentFacade.ContentException("Format error in stream " + streamName, e);
    } catch (MarkUnsupportedException e) {
      throw new ContentFacade.ContentException("Could not create PropertySet for stream " + streamName, e);
    } catch (UnsupportedEncodingException e) {
      throw new ContentFacade.ContentException("Unsupported encoding in stream: " + streamName, e);
    } catch (IOException e) {
      throw new ContentFacade.ContentException("Could not read stream " + streamName, e);
    }

  }

}
