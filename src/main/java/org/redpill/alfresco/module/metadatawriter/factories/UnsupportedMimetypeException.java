package org.redpill.alfresco.module.metadatawriter.factories;

public class UnsupportedMimetypeException extends Exception {

  private static final long serialVersionUID = -1808676031859435205L;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public UnsupportedMimetypeException(final String message) {
    super(message);
  }

}
