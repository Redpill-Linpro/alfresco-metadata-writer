package org.redpill.alfresco.module.metadatawriter.services.poix;

import java.io.Serializable;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;

public interface POIXFieldUpdateSpecification {

  void update(String field, Serializable value, POIXFacade facade) throws ContentException;

}
