package org.redpill.alfresco.module.metadatawriter.services.impl;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;
import org.redpill.alfresco.module.metadatawriter.services.odf.impl.OdfContentFacade;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Component("metadata-writer.OdfInstantiator")
public class OpenDocumentFormatContentInstantiator implements MetadataContentInstantiator {
  private static final Logger LOG = Logger.getLogger(OpenDocumentFormatContentInstantiator.class);

  @Override
  public ContentFacade create(ContentReader reader, ContentWriter writer) throws IOException {
    InputStream contentInputStream = null;
    OutputStream contentOutputStream = null;
    try {
      contentInputStream = reader.getContentInputStream();
      contentOutputStream = writer.getContentOutputStream();
      return create(contentInputStream, contentOutputStream);
    } catch (Throwable e) {
      LOG.trace("Closing streams");
      IOUtils.closeQuietly(contentInputStream);
      IOUtils.closeQuietly(contentOutputStream);
      throw new IOException(e);
    }
  }

  @Override
  public ContentFacade create(InputStream inputStream, OutputStream outputStream) throws IOException {
    return new OdfContentFacade(inputStream, outputStream);
  }

  @Override
  public boolean supports(String mimetype) {
    return MimetypeMap.MIMETYPE_OPENDOCUMENT_TEXT.equalsIgnoreCase(mimetype) || MimetypeMap.MIMETYPE_OPENDOCUMENT_SPREADSHEET.equalsIgnoreCase(mimetype)
      || MimetypeMap.MIMETYPE_OPENDOCUMENT_PRESENTATION.equalsIgnoreCase(mimetype) || MimetypeMap.MIMETYPE_OPENDOCUMENT_CHART.equalsIgnoreCase(mimetype)
      || MimetypeMap.MIMETYPE_OPENDOCUMENT_DATABASE.equalsIgnoreCase(mimetype) || MimetypeMap.MIMETYPE_OPENDOCUMENT_FORMULA.equalsIgnoreCase(mimetype)
      || MimetypeMap.MIMETYPE_OPENDOCUMENT_GRAPHICS.equalsIgnoreCase(mimetype) || MimetypeMap.MIMETYPE_OPENDOCUMENT_IMAGE.equalsIgnoreCase(mimetype);
  }

}
