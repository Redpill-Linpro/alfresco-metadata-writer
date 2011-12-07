package org.redpill.alfresco.module.metadatawriter.services.pdfbox;

import java.io.Serializable;

import org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl.PdfboxFacade;

public interface FieldUpdateSpecification {

  void update(String field, Serializable value, PdfboxFacade facade);

}
