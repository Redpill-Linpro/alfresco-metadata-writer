package org.redpill.alfresco.module.metadatawriter.services.odf.impl;

import java.io.Serializable;
import java.util.Date;

import org.redpill.alfresco.module.metadatawriter.services.odf.OdfFacade;
import org.redpill.alfresco.module.metadatawriter.services.odf.OdfFieldUpdateSpecification;

public enum OdfMetadata {

  AUTHOR("Author", new OdfFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final OdfFacade facade) {
      facade.setAuthor((String) value);
    }

  }), TITLE("Title", new OdfFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final OdfFacade facade) {
      facade.setTitle((String) value);
    }

  }), KEYWORDS("Keywords", new OdfFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final OdfFacade facade) {
      facade.setKeywords((String) value);
    }
  }), CREATE_DATETIME("CreateDateTime", new OdfFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final OdfFacade facade) {
      facade.setCreateDateTime((Date) value);
    }
  }), CUSTOM("Custom", new OdfFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final OdfFacade facade) {
      facade.setCustomMetadata(field, (String) value);
    }
  });

  private final String fieldName;
  private final OdfFieldUpdateSpecification spec;

  private OdfMetadata(final String fieldName, final OdfFieldUpdateSpecification spec) {
    assert null != fieldName;
    assert null != spec;

    this.fieldName = fieldName;
    this.spec = spec;
  }

  private boolean correspondsTo(final String fieldName) {
    // TODO: Translation...
    return this.fieldName.equalsIgnoreCase(fieldName);
  }

  public void update(final String field, final Serializable value, final OdfFacade facade) {
    assert null != value;
    assert null != facade;

    spec.update(field, value, facade);
  }

  static OdfMetadata find(final String field) {
    for (final OdfMetadata metadataField : OdfMetadata.values()) {
      if (metadataField.correspondsTo(field)) {
        return metadataField;
      }
    }

    return CUSTOM;
  }

}