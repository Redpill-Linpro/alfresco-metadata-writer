package org.redpill.alfresco.module.metadatawriter.services.poifs;

import java.io.Serializable;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;

public interface POIFSFieldUpdateSpecification {

  void update(String field, Serializable value, POIFSFacade facade) throws ContentException;

}
