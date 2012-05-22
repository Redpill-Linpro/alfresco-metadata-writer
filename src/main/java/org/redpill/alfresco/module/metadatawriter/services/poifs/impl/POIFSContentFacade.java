package org.redpill.alfresco.module.metadatawriter.services.poifs.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.poifs.POIFSFacade;

public class POIFSContentFacade implements ContentFacade {

  private static final Log logger = LogFactory.getLog(POIFSContentFacade.class);

  private final POIFSFacade poifsFacade;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public POIFSContentFacade(final InputStream in, final OutputStream out) throws IOException {
    this.poifsFacade = new POIFSFacadeImpl(in, out);
  }

  // For unit testing purposes
  protected POIFSContentFacade(final POIFSFacade poifsFacade) {
    this.poifsFacade = poifsFacade;
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------
  @Override
  public void save() throws ContentException {

    if (logger.isDebugEnabled()) {
      logger.debug("Saving content");
    }

    try {
      poifsFacade.writeProperties();
    } catch (final IOException e) {
      throw new ContentException("Could not save metadata", e);
    }
  }

  @Override
  public void abort() throws ContentException {
    try {
      poifsFacade.close();
    } catch (final IOException ioe) {
      throw new ContentException("Unable to abort the POIFSFacade!", ioe);
    }
  }

  @Override
  public void writeMetadata(final String field, final Serializable value) throws ContentException {

    if (logger.isDebugEnabled()) {
      logger.debug("Exporting metadata " + field + " with value " + value);
    }

    final POIFSMetadata metadata = POIFSMetadata.find(field);

    try {
      metadata.update(field, value, poifsFacade);
    } catch (final ContentException e) {
      throw new ContentException("Could not export metadata " + field + " with value " + value, e);
    }
  }

}
