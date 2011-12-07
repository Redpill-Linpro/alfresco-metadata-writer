package org.redpill.alfresco.module.metadatawriter.services.msoffice.impl;

import java.io.Serializable;
import java.util.Date;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.msoffice.FieldUpdateSpecification;
import org.redpill.alfresco.module.metadatawriter.services.msoffice.POIFSFacade;

public enum MSOfficeMetadata {

  AUTHOR("Author", new FieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setAuthor((String) value);
    }

  }), TITLE("Title", new FieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setTitle((String) value);
    }

  }), KEYWORDS("Keywords", new FieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setKeywords((String) value);
    }
  }), CREATE_DATETIME("CreateDateTime", new FieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setCreateDateTime((Date) value);
    }
  }), CUSTOM("Custom", new FieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setCustomMetadata(field, (String) value);
    }
  });

  private final String fieldName;
  private final FieldUpdateSpecification spec;

  private MSOfficeMetadata(final String fieldName, final FieldUpdateSpecification spec) {
    assert null != fieldName;
    assert null != spec;

    this.fieldName = fieldName;
    this.spec = spec;
  }

  private boolean correspondsTo(final String fieldName) {
    // TODO: Translation...
    return this.fieldName.equalsIgnoreCase(fieldName);
  }

  public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
    assert null != value;
    assert null != facade;

    spec.update(field, value, facade);

  }

  static MSOfficeMetadata find(final String field) {
    for (final MSOfficeMetadata metadataField : MSOfficeMetadata.values()) {
      if (metadataField.correspondsTo(field)) {
        return metadataField;
      }
    }

    return CUSTOM;
  }

}