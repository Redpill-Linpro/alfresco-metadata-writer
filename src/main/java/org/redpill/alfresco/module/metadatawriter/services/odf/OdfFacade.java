package org.redpill.alfresco.module.metadatawriter.services.odf;

import java.util.Date;

public interface OdfFacade extends AutoCloseable {

  void writeProperties();

  void setTitle(String title);

  void setAuthor(String author);

  void setKeywords(String keywords);

  void setCreateDateTime(Date dateTime);

  void setCustomMetadata(String field, String value);

}
