package org.redpill.alfresco.module.metadatawriter.services;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;

public interface MetadataContentInstantiator {

  ContentFacade create(ContentReader reader, ContentWriter writer) throws IOException;
  
  ContentFacade create(InputStream inputStream, OutputStream outputStream) throws IOException;

  boolean supports(String mimetype);

}
