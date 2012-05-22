package org.redpill.alfresco.module.metadatawriter.services.docx4j.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.docx4j.Docx4jFacade;

public class Docx4jContentFacade implements ContentFacade {

  private static final Log LOG = LogFactory.getLog(Docx4jContentFacade.class);

  private final Docx4jFacade _docx4jFacade;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public Docx4jContentFacade(final InputStream in, final OutputStream out) throws IOException {
    _docx4jFacade = new Docx4jFacadeImpl(in, out);
  }

  // For unit testing purposes
  protected Docx4jContentFacade(final Docx4jFacade docx4jFacade) {
    _docx4jFacade = docx4jFacade;
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
      _docx4jFacade.writeProperties();
    } catch (final IOException e) {
      throw new ContentException("Could not save metadata", e);
    }
  }

  @Override
  public void abort() throws ContentException {
    try {
      _docx4jFacade.close();
    } catch (final IOException ioe) {
      throw new ContentException("Unable to abort the Docx4jFacade!", ioe);
    }
  }

  @Override
  public void writeMetadata(final String field, final Serializable value) throws ContentException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + " with value " + value);
    }

    final Docx4jMetadata metadata = Docx4jMetadata.find(field);

    try {
      metadata.update(field, value, _docx4jFacade);
    } catch (final ContentException e) {
      throw new ContentException("Could not export metadata " + field + " with value " + value, e);
    }
  }

}
