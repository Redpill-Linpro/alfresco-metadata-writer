package org.redpill.alfresco.module.metadatawriter.services.docx4j;

import java.io.Serializable;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;

public interface Docx4jFieldUpdateSpecification {

  void update(String field, Serializable value, Docx4jFacade facade) throws ContentException;

}
