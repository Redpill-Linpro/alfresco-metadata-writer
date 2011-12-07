package org.redpill.alfresco.module.metadatawriter.model;

import org.alfresco.service.namespace.QName;

public interface MetadataWriterModel {

  public static final String URI = "http://www.redpill.se/model/metadata-writer/1.0";

  public final QName ASPECT_METADATA_WRITEABLE = QName.createQName(URI, "metadatawriteable");
  public final QName PROP_METADATA_SERVICE_NAME = QName.createQName(URI, "serviceName");

}
