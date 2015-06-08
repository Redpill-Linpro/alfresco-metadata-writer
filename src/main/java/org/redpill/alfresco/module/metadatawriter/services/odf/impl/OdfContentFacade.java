package org.redpill.alfresco.module.metadatawriter.services.odf.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.odf.OdfFacade;

public class OdfContentFacade implements ContentFacade {

  private static final Log LOG = LogFactory.getLog(OdfContentFacade.class);

  private OdfFacade _odfFacade;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public OdfContentFacade(InputStream in, OutputStream out) {
    _odfFacade = new OdfFacadeImpl(in, out);
  }

  // For unit testing purposes
  protected OdfContentFacade(OdfFacade odfFacade) {
    _odfFacade = odfFacade;
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------
  @Override
  public void save() {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Saving content");
    }

    _odfFacade.writeProperties();
  }

  @Override
  public void abort() {
    try {
      _odfFacade.close();
    } catch (Exception ex) {
      throw new RuntimeException("Unable to abort the OdfFacade!", ex);
    }
  }

  @Override
  public void writeMetadata(String field, Serializable value) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + " with value " + value);
    }

    OdfMetadata metadata = OdfMetadata.find(field);

    metadata.update(field, value, _odfFacade);
  }

}
