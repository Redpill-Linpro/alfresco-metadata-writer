package org.redpill.alfresco.module.metadatawriter.services.poifs.impl;

import java.io.Serializable;
import java.util.Date;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.poifs.POIFSFieldUpdateSpecification;
import org.redpill.alfresco.module.metadatawriter.services.poifs.POIFSFacade;

public enum POIFSMetadata {

  AUTHOR("Author", new POIFSFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setAuthor((String) value);
    }

  }), TITLE("Title", new POIFSFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setTitle((String) value);
    }

  }), KEYWORDS("Keywords", new POIFSFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setKeywords((String) value);
    }
  }), CREATE_DATETIME("CreateDateTime", new POIFSFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setCreateDateTime((Date) value);
    }
  }), CUSTOM("Custom", new POIFSFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIFSFacade facade) throws ContentException {
      facade.setCustomMetadata(field, (String) value);
    }
  });

  private final String fieldName;
  private final POIFSFieldUpdateSpecification spec;

  private POIFSMetadata(final String fieldName, final POIFSFieldUpdateSpecification spec) {
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

  static POIFSMetadata find(final String field) {
    for (final POIFSMetadata metadataField : POIFSMetadata.values()) {
      if (metadataField.correspondsTo(field)) {
        return metadataField;
      }
    }

    return CUSTOM;
  }

}