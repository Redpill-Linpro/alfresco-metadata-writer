package org.redpill.alfresco.module.metadatawriter.services.msoffice;

import java.io.Serializable;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;

public interface FieldUpdateSpecification {

  void update(String field, Serializable value, POIFSFacade facade) throws ContentException;

}
