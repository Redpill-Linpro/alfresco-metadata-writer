package org.redpill.alfresco.module.metadatawriter.services.impl;

import java.io.IOException;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;
import org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl.PdfboxFacade;

public class PdfContentInstantiator implements MetadataContentInstantiator {

  @Override
  public ContentFacade create(final ContentReader reader, final ContentWriter writer) throws IOException {
    return new PdfboxFacade(reader.getContentInputStream(), writer.getContentOutputStream());
  }

  @Override
  public boolean supports(final String mimetype) {
    return MimetypeMap.MIMETYPE_PDF.equalsIgnoreCase(mimetype);
  }

}
