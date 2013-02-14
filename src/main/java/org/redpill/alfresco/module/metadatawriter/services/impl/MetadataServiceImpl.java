package org.redpill.alfresco.module.metadatawriter.services.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.converters.ValueConverter;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataServiceRegistry;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;

public class MetadataServiceImpl implements MetadataService {

  private static final Logger LOG = Logger.getLogger(MetadataServiceImpl.class);

  private Map<QName, String> _metadataMapping;
  private MetadataServiceRegistry _registry;
  private MetadataContentFactory _metadataContentFactory;
  private NamespaceService _namespaceService;
  private String _serviceName;
  private List<ValueConverter> _converters;
  private TransactionService _transactionService;
  private BehaviourFilter _behaviourFilter;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public MetadataServiceImpl(final MetadataServiceRegistry registry, final MetadataContentFactory metadataContentFactory,
                             final NamespaceService namespaceService, TransactionService transactionService, BehaviourFilter behaviourFilter, final Properties mappings, final String serviceName, final List<ValueConverter> converters) {
    _registry = registry;
    _metadataContentFactory = metadataContentFactory;
    _namespaceService = namespaceService;
    _transactionService = transactionService;
    _behaviourFilter = behaviourFilter;
    _serviceName = serviceName;
    _converters = converters;
    _metadataMapping = convertMappings(mappings);
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  @Override
  public String getServiceName() {
    return _serviceName;
  }

  @Override
  public void write(final NodeRef contentRef, final Map<QName, Serializable> properties) throws UpdateMetadataException {
    RetryingTransactionHelper.RetryingTransactionCallback<Void> callback = new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {

      public Void execute() throws Throwable {

    	  /*
    	   * Change 130214 Carl Nordenfelt: Needed to disable all behaviours since versioning was
    	   * triggered for the VERSIONABLE_ASPECT behaviour even though it was disabled. Otherwise
    	   * a new version is created when the updated content is written (and cm:autoVersion == true)
    	   */
    	_behaviourFilter.disableAllBehaviours();  
    	  
        assert contentRef != null;
        assert properties != null;

        final ContentFacade content;
        try {
          content = _metadataContentFactory.createContent(contentRef);
        } catch (final IOException ioe) {
          throw new UpdateMetadataException("Could not create metadata content from node " + contentRef, ioe);
        } catch (final UnsupportedMimetypeException ume) {
          throw new UpdateMetadataException("Could not create metadata content for unknown mimetype!", ume);
        }

        final Map<String, Serializable> propertyMap = createPropertyMap(properties);

        for (final Map.Entry<String, Serializable> property : propertyMap.entrySet()) {

          final Serializable value = convert(property.getValue());

          try {
            content.writeMetadata(property.getKey(), value);
          } catch (final ContentException e) {
            LOG.warn("Could not export property " + property.getKey() + " with value " + value, e);

            try {
              content.abort();
            } catch (final ContentException ce) {
              throw new AlfrescoRuntimeException("Unable to abort the metadata write!", ce);
            }

            return null;
          }
        }

        try {
          content.save();
        } catch (final ContentException e) {
          throw new UpdateMetadataException("Could not save after update!", e);
        }

        return null;
      }

    };

    RetryingTransactionHelper transactionHelper = _transactionService.getRetryingTransactionHelper();

    transactionHelper.doInTransaction(callback, false, true);
  }

  public void register() {
    _registry.register(this);
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private Map<QName, String> convertMappings(final Properties mapping) {

    final Map<QName, String> convertedMapping = new HashMap<QName, String>(mapping.size());

    for (final Map.Entry<Object, Object> entry : mapping.entrySet()) {
      final String qnameStr = (String) entry.getValue();
      final String propertyName = (String) entry.getKey();

      try {
        final QName qName = QName.createQName(qnameStr, _namespaceService);
        convertedMapping.put(qName, propertyName);
      } catch (final NamespaceException ne) {
        LOG.warn("Could not create QName for " + qnameStr + ", cause: " + ne);
      }

    }

    return convertedMapping;
  }

  private final Serializable convert(final Serializable value) {

    for (final ValueConverter c : _converters) {
      if (c.applicable(value)) {
        return c.convert(value);
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Did not find any converter for value " + value);
    }

    return value;

  }

  private Map<String, Serializable> createPropertyMap(final Map<QName, Serializable> properties) {
    final HashMap<String, Serializable> propertyMap = new HashMap<String, Serializable>(properties.size());

    for (final Map.Entry<QName, Serializable> p : properties.entrySet()) {

      if (_metadataMapping.containsKey(p.getKey())) {
        final Serializable extractedValue = PropertyValueExtractor.extractValue(p.getValue());

        if (extractedValue != null) {
          propertyMap.put(_metadataMapping.get(p.getKey()), extractedValue);
        }
      } else {
        // this property should not be written!
        if (LOG.isTraceEnabled()) {
          LOG.trace("Metadata " + p.getKey() + " with value " + p.getValue() + " is not mapped and being ignored!");
        }
      }
    }

    return propertyMap;

  }

}
