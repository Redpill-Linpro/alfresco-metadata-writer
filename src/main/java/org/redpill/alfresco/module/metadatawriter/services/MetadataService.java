package org.redpill.alfresco.module.metadatawriter.services;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public interface MetadataService {

  void write(NodeRef contentRef, Map<QName, Serializable> properties) throws UpdateMetadataException;

  String getServiceName();

  class UpdateMetadataException extends Exception {
    private static final long serialVersionUID = 5154476024580712311L;

    public UpdateMetadataException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

}
