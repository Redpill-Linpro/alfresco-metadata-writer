package org.redpill.alfresco.module.metadatawriter.factories.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.util.ParameterCheck;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.StringUtils;

public class MetadataContentFactoryImpl implements MetadataContentFactory {

  @Autowired
  @Qualifier("ContentService")
  private ContentService _contentService;

  private Set<MetadataContentInstantiator> _instantiators;

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------
  @Override
  public ContentFacade createContent(NodeRef contentNode) throws UnsupportedMimetypeException, IOException {
    assert null != contentNode;

    String mimetype = findMimetype(getContentReader(contentNode));

    if (!StringUtils.hasText(mimetype)) {
      throw new ContentIOException("Mimetype not specified for node " + contentNode);
    }

    MetadataContentInstantiator instantiator = getMetadataContentInstantiator(mimetype);

    if (instantiator == null) {
      throw new UnsupportedMimetypeException("This MetadataContentFactory does not support mimetype " + mimetype);
    }
    ContentFacade result = null;
    result = instantiator.create(getContentReader(contentNode), getContentWriter(contentNode));

    return result;
  }

  @Override
  public ContentFacade createContent(InputStream inputStream, OutputStream outputStream, String mimetype) throws UnsupportedMimetypeException, IOException {
    ParameterCheck.mandatory("mimetype", mimetype);

    MetadataContentInstantiator instantiator = getMetadataContentInstantiator(mimetype);

    if (instantiator == null) {
      throw new UnsupportedMimetypeException("This MetadataContentFactory does not support mimetype " + mimetype);
    }

    return instantiator.create(inputStream, outputStream);
  }

  @Override
  public boolean supportsMetadataWrite(NodeRef contentNode) {
    assert null != contentNode;

    String mimetype = findMimetype(getContentReader(contentNode));

    if (!StringUtils.hasText(mimetype)) {
      return false;
    }

    return getMetadataContentInstantiator(mimetype) != null;
  }

  private MetadataContentInstantiator getMetadataContentInstantiator(String mimetype) {
    for (MetadataContentInstantiator instantiator : _instantiators) {
      if (instantiator.supports(mimetype)) {
        return instantiator;
      }
    }

    return null;
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private static String findMimetype(ContentReader contentReader) {
    if (null == contentReader) {
      throw new ContentIOException("ContentReader must be supplied!");
    }

    return contentReader.getMimetype();
  }

  private ContentReader getContentReader(NodeRef contentNode) {
    ContentReader reader = _contentService.getReader(contentNode, ContentModel.PROP_CONTENT);

    // The reader may be null, e.g. for folders and the like
    if (reader == null || !reader.exists()) {
      throw new ContentIOException("Could not get ContentReader from node " + contentNode);
    }

    return reader;
  }

  private ContentWriter getContentWriter(NodeRef contentNode) {
    ContentWriter writer = _contentService.getWriter(contentNode, ContentModel.PROP_CONTENT, true);

    if (writer == null) {
      throw new ContentIOException("Could not get ContentWriter from node " + contentNode);
    }

    return writer;
  }

  public void setInstantiators(Set<MetadataContentInstantiator> instantiators) {
    _instantiators = instantiators;
  }

  public void setContentService(ContentService contentService) {
    _contentService = contentService;
  }

}
