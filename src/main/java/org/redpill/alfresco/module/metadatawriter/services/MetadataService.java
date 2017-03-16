package org.redpill.alfresco.module.metadatawriter.services;

import org.alfresco.service.cmr.repository.NodeRef;

public interface MetadataService {
  
  public static final int DEFAULT_TIMEOUT = 120000;

  void writeSynchronized(NodeRef nodeRef) throws UpdateMetadataException;
  
  void write(NodeRef contentRef) throws UpdateMetadataException;

  void write(NodeRef nodeRef, MetadataWriterCallback callback) throws UpdateMetadataException;

  String getServiceName();

  class UpdateMetadataException extends Exception {
    private static final long serialVersionUID = 5154476024580712311L;

    public UpdateMetadataException(final String message, final Throwable cause) {
      super(message, cause);
    }
  }

}
