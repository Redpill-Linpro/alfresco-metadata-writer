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

  private POIFSFacade poifsFacade;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public POIFSContentFacade(InputStream in, OutputStream out) throws IOException {
    this.poifsFacade = new POIFSFacadeImpl(in, out);
  }

  // For unit testing purposes
  protected POIFSContentFacade(POIFSFacade poifsFacade) {
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
    } catch (IOException e) {
      throw new ContentException("Could not save metadata", e);
    } 
  }

  @Override
  public void abort() throws ContentException {
    try {
      poifsFacade.close();
    } catch (IOException ioe) {
      throw new ContentException("Unable to abort the POIFSFacade!", ioe);
    }
  }

  @Override
  public void writeMetadata(String field, Serializable value) throws ContentException {

    if (logger.isDebugEnabled()) {
      logger.debug("Exporting metadata " + field + " with value " + value);
    }

    POIFSMetadata metadata = POIFSMetadata.find(field);

    try {
      metadata.update(field, value, poifsFacade);
    } catch (ContentException e) {
      throw new ContentException("Could not export metadata " + field + " with value " + value, e);
    }
  }

}
