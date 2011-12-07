package org.redpill.alfresco.module.metadatawriter.services.impl;

import java.io.IOException;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;
import org.redpill.alfresco.module.metadatawriter.services.msoffice.impl.MSOfficeFacade;

public class DefaultContentInstantiator {

  // ---------------------------------------------------
  // Public classes
  // ---------------------------------------------------
  public static class MSWordContentInstantiator implements MetadataContentInstantiator {

    @Override
    public boolean supports(final String mimetype) {
      return MimetypeMap.MIMETYPE_WORD.equalsIgnoreCase(mimetype);
    }

    @Override
    public ContentFacade create(final ContentReader reader, final ContentWriter writer) throws IOException {
      return new MSOfficeFacade(reader.getContentInputStream(), writer.getContentOutputStream());
    }
  }

  public static class MSExcelContentInstantiator implements MetadataContentInstantiator {

    @Override
    public boolean supports(final String mimetype) {
      return MimetypeMap.MIMETYPE_EXCEL.equalsIgnoreCase(mimetype);
    }

    @Override
    public ContentFacade create(final ContentReader reader, final ContentWriter writer) throws IOException {
      return new MSOfficeFacade(reader.getContentInputStream(), writer.getContentOutputStream());
    }
  }

  public static class MSPowerPointContentInstantiator implements MetadataContentInstantiator {

    @Override
    public boolean supports(final String mimetype) {
      return MimetypeMap.MIMETYPE_PPT.equalsIgnoreCase(mimetype);
    }

    @Override
    public ContentFacade create(final ContentReader reader, final ContentWriter writer) throws IOException {
      return new MSOfficeFacade(reader.getContentInputStream(), writer.getContentOutputStream());
    }
  }

}
