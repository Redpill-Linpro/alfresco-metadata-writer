package org.redpill.alfresco.module.metadatawriter.factories;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.alfresco.service.cmr.repository.NodeRef;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;

public interface MetadataContentFactory {

  ContentFacade createContent(NodeRef contentNode) throws UnsupportedMimetypeException, IOException;
  
  ContentFacade createContent(InputStream inputStream, OutputStream outputStream, String mimetype) throws UnsupportedMimetypeException, IOException;

  boolean supportsMetadataWrite(NodeRef contentNode);


}
