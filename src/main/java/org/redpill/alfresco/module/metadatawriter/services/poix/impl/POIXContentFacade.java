package org.redpill.alfresco.module.metadatawriter.services.poix.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.poix.POIXFacade;

public class POIXContentFacade implements ContentFacade {

  private static final Log LOG = LogFactory.getLog(POIXContentFacade.class);

  private POIXFacade _poixFacade;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public POIXContentFacade(InputStream in, OutputStream out) throws IOException {
    _poixFacade = new POIXFacadeImpl(in, out);
  }

  // For unit testing purposes
  protected POIXContentFacade(POIXFacade poixFacade) {
    _poixFacade = poixFacade;
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------
  @Override
  public void save() throws ContentException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Saving content");
    }

    try {
      _poixFacade.writeProperties();
    } catch (IOException e) {
      throw new ContentException("Could not save metadata", e);
    }
  }

  @Override
  public void abort() throws ContentException {
    try {
      _poixFacade.close();
    } catch (IOException ioe) {
      throw new ContentException("Unable to abort the POIXFacade!", ioe);
    }
  }

  @Override
  public void writeMetadata(String field, Serializable value) throws ContentException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + " with value " + value);
    }

    POIXMetadata metadata = POIXMetadata.find(field);

    try {
      metadata.update(field, value, _poixFacade);
    } catch (ContentException e) {
      throw new ContentException("Could not export metadata " + field + " with value " + value, e);
    }
  }

}
