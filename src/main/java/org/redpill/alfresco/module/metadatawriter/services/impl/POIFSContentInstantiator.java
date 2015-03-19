package org.redpill.alfresco.module.metadatawriter.services.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;
import org.redpill.alfresco.module.metadatawriter.services.poifs.impl.POIFSContentFacade;

public class POIFSContentInstantiator {

  public static abstract class AbstractPOIFSContentInstantiator implements MetadataContentInstantiator {

    @Override
    public ContentFacade create(InputStream inputStream, OutputStream outputStream) throws IOException {
      return new POIFSContentFacade(inputStream, outputStream);
    }

    @Override
    public ContentFacade create(ContentReader reader, ContentWriter writer) throws IOException {
      return create(reader.getContentInputStream(), writer.getContentOutputStream());
    }

  }

  // ---------------------------------------------------
  // Public classes
  // ---------------------------------------------------
  public static class MSWordContentInstantiator extends AbstractPOIFSContentInstantiator {

    @Override
    public boolean supports(String mimetype) {
      return MimetypeMap.MIMETYPE_WORD.equalsIgnoreCase(mimetype);
    }

  }

  public static class MSExcelContentInstantiator extends AbstractPOIFSContentInstantiator {

    @Override
    public boolean supports(String mimetype) {
      return MimetypeMap.MIMETYPE_EXCEL.equalsIgnoreCase(mimetype);
    }

  }

  public static class MSPowerPointContentInstantiator extends AbstractPOIFSContentInstantiator {

    @Override
    public boolean supports(String mimetype) {
      return MimetypeMap.MIMETYPE_PPT.equalsIgnoreCase(mimetype);
    }

  }

}
