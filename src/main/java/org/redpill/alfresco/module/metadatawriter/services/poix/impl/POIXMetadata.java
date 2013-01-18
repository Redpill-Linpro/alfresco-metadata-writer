package org.redpill.alfresco.module.metadatawriter.services.poix.impl;

import java.io.Serializable;
import java.util.Date;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.poix.POIXFacade;
import org.redpill.alfresco.module.metadatawriter.services.poix.POIXFieldUpdateSpecification;

public enum POIXMetadata {

  AUTHOR("Author", new POIXFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIXFacade facade) throws ContentException {
      facade.setAuthor((String) value);
    }

  }), TITLE("Title", new POIXFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIXFacade facade) throws ContentException {
      facade.setTitle((String) value);
    }

  }), KEYWORDS("Keywords", new POIXFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIXFacade facade) throws ContentException {
      facade.setKeywords((String) value);
    }
  }), CREATE_DATETIME("CreateDateTime", new POIXFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIXFacade facade) throws ContentException {
      facade.setCreateDateTime((Date) value);
    }
  }), CUSTOM("Custom", new POIXFieldUpdateSpecification() {
    @Override
    public void update(final String field, final Serializable value, final POIXFacade facade) throws ContentException {
      facade.setCustomMetadata(field, (String) value);
    }
  });

  private final String fieldName;
  private final POIXFieldUpdateSpecification spec;

  private POIXMetadata(final String fieldName, final POIXFieldUpdateSpecification spec) {
    assert null != fieldName;
    assert null != spec;

    this.fieldName = fieldName;
    this.spec = spec;
  }

  private boolean correspondsTo(final String fieldName) {
    // TODO: Translation...
    return this.fieldName.equalsIgnoreCase(fieldName);
  }

  public void update(final String field, final Serializable value, final POIXFacade facade) throws ContentException {
    assert null != value;
    assert null != facade;

    spec.update(field, value, facade);
  }

  static POIXMetadata find(final String field) {
    for (final POIXMetadata metadataField : POIXMetadata.values()) {
      if (metadataField.correspondsTo(field)) {
        return metadataField;
      }
    }

    return CUSTOM;
  }

}