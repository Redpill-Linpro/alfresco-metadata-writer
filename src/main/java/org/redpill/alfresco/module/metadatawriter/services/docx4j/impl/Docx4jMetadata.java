package org.redpill.alfresco.module.metadatawriter.services.docx4j.impl;

import java.io.Serializable;
import java.util.Date;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.docx4j.Docx4jFacade;
import org.redpill.alfresco.module.metadatawriter.services.docx4j.Docx4jFieldUpdateSpecification;

public enum Docx4jMetadata {

  AUTHOR("Author", new Docx4jFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final Docx4jFacade facade) throws ContentException {
      facade.setAuthor((String) value);
    }

  }), TITLE("Title", new Docx4jFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final Docx4jFacade facade) throws ContentException {
      facade.setTitle((String) value);
    }

  }), KEYWORDS("Keywords", new Docx4jFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final Docx4jFacade facade) throws ContentException {
      facade.setKeywords((String) value);
    }
  }), CREATE_DATETIME("CreateDateTime", new Docx4jFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final Docx4jFacade facade) throws ContentException {
      facade.setCreateDateTime((Date) value);
    }
  }), CUSTOM("Custom", new Docx4jFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final Docx4jFacade facade) throws ContentException {
      facade.setCustomMetadata(field, (String) value);
    }
  });

  private final String fieldName;
  private final Docx4jFieldUpdateSpecification spec;

  private Docx4jMetadata(final String fieldName, final Docx4jFieldUpdateSpecification spec) {
    assert null != fieldName;
    assert null != spec;

    this.fieldName = fieldName;
    this.spec = spec;
  }

  private boolean correspondsTo(final String fieldName) {
    // TODO: Translation...
    return this.fieldName.equalsIgnoreCase(fieldName);
  }

  public void update(final String field, final Serializable value, final Docx4jFacade facade) throws ContentException {
    assert null != value;
    assert null != facade;

    spec.update(field, value, facade);
  }

  static Docx4jMetadata find(final String field) {
    for (final Docx4jMetadata metadataField : Docx4jMetadata.values()) {
      if (metadataField.correspondsTo(field)) {
        return metadataField;
      }
    }

    return CUSTOM;
  }

}