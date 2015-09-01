package org.redpill.alfresco.module.metadatawriter;

public class InvalidFormatException extends RuntimeException {

  public InvalidFormatException(org.apache.poi.openxml4j.exceptions.InvalidFormatException ex) {
    super(ex);
  }

  private static final long serialVersionUID = 3735668604955364357L;

}
