package org.redpill.alfresco.module.metadatawriter.factories;

import java.io.IOException;

import org.alfresco.service.cmr.repository.NodeRef;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;

public interface MetadataContentFactory {

  ContentFacade createContent(NodeRef contentNode) throws UnsupportedMimetypeException, IOException;

}
