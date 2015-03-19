package org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl;

import java.io.Serializable;
import java.util.Date;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.pdfbox.FieldUpdateSpecification;

public enum PdfboxMetadata {

  AUTHOR("Author", new FieldUpdateSpecification() {

    @Override
    public void update(final String field, final Serializable value, final PdfboxFacade facade) {
      facade.setAuthor((String) value);
    }

  }), TITLE("Title", new FieldUpdateSpecification() {

    @Override
    public void update(final String field, final Serializable value, final PdfboxFacade facade) {
      facade.setTitle(value != null ? value.toString() : null);
    }

  }), KEYWORDS("Keywords", new FieldUpdateSpecification() {

    @Override
    public void update(final String field, final Serializable value, final PdfboxFacade facade) {
      facade.setKeywords((String) value);
    }

  }), CREATE_DATETIME("CreateDateTime", new FieldUpdateSpecification() {

    @Override
    public void update(final String field, final Serializable value, final PdfboxFacade facade) {
      facade.setCreateDateTime((Date) value);
    }

  }), CUSTOM("Custom", new FieldUpdateSpecification() {

    @Override
    public void update(final String field, final Serializable value, final PdfboxFacade facade) {
      facade.setCustomMetadata(field, (String) value);
    }

  });

  private final String _fieldname;
  private final FieldUpdateSpecification _specification;

  private PdfboxMetadata(final String fieldname, final FieldUpdateSpecification specification) {
    assert null != fieldname;
    assert null != specification;

    _fieldname = fieldname;
    _specification = specification;
  }

  private boolean correspondsTo(final String fieldname) {
    return _fieldname.equalsIgnoreCase(fieldname);
  }

  public void update(final String field, final Serializable value, final PdfboxFacade facade) throws ContentException {
    assert null != value;
    assert null != facade;

    _specification.update(field, value, facade);
  }

  static PdfboxMetadata find(final String field) {
    for (final PdfboxMetadata metadataField : PdfboxMetadata.values()) {
      if (metadataField.correspondsTo(field)) {
        return metadataField;
      }
    }

    return CUSTOM;
  }

}
