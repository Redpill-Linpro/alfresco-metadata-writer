package org.redpill.alfresco.module.metadatawriter.services.odf;

import java.io.Serializable;

public interface OdfFieldUpdateSpecification {

  void update(String field, Serializable value, OdfFacade facade);

}
