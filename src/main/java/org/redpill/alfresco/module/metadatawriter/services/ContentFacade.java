package org.redpill.alfresco.module.metadatawriter.services;

import java.io.Serializable;

public interface ContentFacade {

  void writeMetadata(String field, Serializable serializable) throws ContentException;

  /**
   * Writes the updated content.
   * 
   * @throws ContentException
   *           If the write operation failed
   */
  void save() throws ContentException;

  public class ContentException extends Exception {

    private static final long serialVersionUID = -5625363611017820280L;

    public ContentException(final String message, final Throwable cause) {
      super(message, cause);
    }

  }

  /**
   * Close any streams to the underlying data. Cleans up dependencies.
   * 
   * @throws ContentException
   */
  void abort() throws ContentException;

}
