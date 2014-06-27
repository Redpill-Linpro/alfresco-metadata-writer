package org.redpill.alfresco.module.metadatawriter.services.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.rendition.executer.DeleteRenditionActionExecuter;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.converters.ValueConverter;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataServiceRegistry;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.model.MetadataWriterModel;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.redpill.alfresco.module.metadatawriter.services.MetadataWriterCallback;
import org.redpill.alfresco.module.metadatawriter.services.NodeMetadataProcessor;
import org.redpill.alfresco.module.metadatawriter.services.NodeVerifierProcessor;

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
  private TransactionListener _transactionListener;
  private NodeService _nodeService;
  private ThreadPoolExecutor _threadPoolExecutor;
  private NodeMetadataProcessor _nodeMetadataProcessor;
  private NodeVerifierProcessor _nodeVerifierProcessor;
  private ActionService _actionService;

  /**
   * Controls if existing renditions will be deleted after a successful metadata
   * write. If renditions are not deleted they may not reflect the actual node
   * content
   */
  private boolean _deleteRenditions = false;

  private static final Object KEY_UPDATER = MetadataService.class.getName() + ".updater";

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public MetadataServiceImpl(final MetadataServiceRegistry registry, final MetadataContentFactory metadataContentFactory, final NamespaceService namespaceService,
      TransactionService transactionService, BehaviourFilter behaviourFilter, final NodeService nodeService, final ActionService actionService) {
    _registry = registry;
    _metadataContentFactory = metadataContentFactory;
    _namespaceService = namespaceService;
    _transactionService = transactionService;
    _behaviourFilter = behaviourFilter;
    _nodeService = nodeService;
    _actionService = actionService;
  }

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public MetadataServiceImpl(final MetadataServiceRegistry registry, final MetadataContentFactory metadataContentFactory, final NamespaceService namespaceService,
      TransactionService transactionService, BehaviourFilter behaviourFilter, final Properties mappings, final String serviceName, final List<ValueConverter> converters, final NodeService nodeService) {
    _registry = registry;
    _metadataContentFactory = metadataContentFactory;
    _namespaceService = namespaceService;
    _transactionService = transactionService;
    _behaviourFilter = behaviourFilter;
    _serviceName = serviceName;
    _converters = converters;
    _metadataMapping = convertMappings(mappings);
    _nodeService = nodeService;
  }

  public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor) {
    _threadPoolExecutor = threadPoolExecutor;
  }

  public void setNodeMetadataProcessor(NodeMetadataProcessor nodeMetadataProcessor) {
    _nodeMetadataProcessor = nodeMetadataProcessor;
  }

  public void setNodeVerifierProcessor(NodeVerifierProcessor nodeVerifierProcessor) {
    _nodeVerifierProcessor = nodeVerifierProcessor;
  }

  public void setServiceName(String serviceName) {
    _serviceName = serviceName;
  }

  public void setConverters(List<ValueConverter> converters) {
    _converters = converters;
  }

  public void setMappings(Properties mappings) {
    _metadataMapping = convertMappings(mappings);
  }

  public void setDeleteRenditions(boolean deleteRenditions) {
    _deleteRenditions = deleteRenditions;
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  @Override
  public String getServiceName() {
    return _serviceName;
  }

  @Override
  public void write(final NodeRef contentRef) throws UpdateMetadataException {
    write(contentRef, null);
  }

  @Override
  public void write(final NodeRef nodeRef, MetadataWriterCallback callback) throws UpdateMetadataException {

    // set up the transaction listener
    _transactionListener = new MetadataWriterTransactionListener();

    AlfrescoTransactionSupport.bindListener(_transactionListener);
    AlfrescoTransactionSupport.bindResource(KEY_UPDATER, new MetadataWriterUpdater(nodeRef, callback));

  }

  public void register() {
    _registry.register(this);
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private void writeNode(final NodeRef contentRef) throws UpdateMetadataException {

    final Map<QName, Serializable> properties = _nodeMetadataProcessor.processNode(contentRef);

    RetryingTransactionHelper.RetryingTransactionCallback<Void> callback = new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {

      public Void execute() throws Throwable {
        if (contentRef == null || !_nodeService.exists(contentRef)) {
          LOG.warn("Node " + contentRef + " does not exist. Aborting writeNode...");
          return null;
        }
        /*
         * Change 130214 Carl Nordenfelt: Needed to disable all behaviours since
         * versioning was triggered for the VERSIONABLE_ASPECT behaviour even
         * though it was disabled. Otherwise a new version is created when the
         * updated content is written (and cm:autoVersion == true)
         */

        // TODO: Remove!
        _behaviourFilter.disableBehaviour();

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

  private void doUpdateProperties(final NodeRef node) {

    if (node == null || !_nodeService.exists(node)) {
      LOG.warn("Node " + node + " does not exist. Aborting doUpdateProperties...");
      return;
    }

    final Serializable failOnUnsupportedValue = _nodeService.getProperty(node, MetadataWriterModel.PROP_METADATA_FAIL_ON_UNSUPPORTED);

    boolean failOnUnsupported = true;

    if (failOnUnsupportedValue != null) {
      failOnUnsupported = (Boolean) failOnUnsupportedValue;
    }

    if (!_nodeVerifierProcessor.verifyDocument(node)) {

      return;
    }

    try {
      _behaviourFilter.disableBehaviour();

      writeNode(node);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Successfully wrote metadata properties on node: " + node);
      }
    } catch (final UpdateMetadataException ume) {
      if (failOnUnsupported) {
        throw new AlfrescoRuntimeException("Could not write properties " + _nodeService.getProperties(node) + " to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME) + "( " + node + ")",
            ume);
      } else {
        LOG.error("Could not write properties " + _nodeService.getProperties(node) + " to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME) + " (" + node + ")", ume);
      }
    } catch (final Exception ex) {
      // catch the general error and log it
      LOG.error("Could not write properties " + _nodeService.getProperties(node) + " to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME) + " (" + node + ")", ex);
    } finally {
      _behaviourFilter.enableBehaviour();
    }
  }

  private class MetadataWriterTransactionListener extends TransactionListenerAdapter {

    @Override
    public void afterCommit() {
      final MetadataWriterUpdater updater = AlfrescoTransactionSupport.getResource(KEY_UPDATER);

      if (updater == null) {
        throw new Error("MetadataWriterUpdater was null after commit!");
      }

      _threadPoolExecutor.execute(updater);

    }
  }

  private class MetadataWriterUpdater implements Runnable {

    private final NodeRef _nodeRef;
    private final MetadataWriterCallback _callback;

    public MetadataWriterUpdater(final NodeRef nodeRef, final MetadataWriterCallback callback) {
      _nodeRef = nodeRef;
      _callback = callback;
    }

    @Override
    public void run() {

      AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<String>() {
        @Override
        public String doWork() throws Exception {
          RetryingTransactionHelper.RetryingTransactionCallback<Void> callback = new RetryingTransactionHelper.RetryingTransactionCallback<Void>() {

            @Override
            public Void execute() throws Throwable {
              doUpdateProperties(_nodeRef);

              return null;
            }

          };

          try {
            RetryingTransactionHelper txnHelper = _transactionService.getRetryingTransactionHelper();

            txnHelper.doInTransaction(callback, false, true);
          } catch (Exception ex) {
            LOG.error("Failed to write metadata properties to node: " + _nodeRef, ex);
          }

          return "";
        }

      }, AuthenticationUtil.SYSTEM_USER_NAME);

      if (_deleteRenditions) {
        deleteRenditions();
      }

      if (_callback != null) {
        _callback.execute();
      }
    }

    /**
     * Makes a call to the action service for each rendition with a delete
     * request.
     */
    private void deleteRenditions() {
      final QName ASSOC_WEBPREVIEW = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "webpreview");
      final QName ASSOC_PDF = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "pdf");
      final QName ASSOC_DOCLIB = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "doclib");

      // Delete web preview
      triggerDeleteRendition(ASSOC_WEBPREVIEW);

      // Delete pdf rendition
      triggerDeleteRendition(ASSOC_PDF);

      // Delete thumbnail (doclib)
      triggerDeleteRendition(ASSOC_DOCLIB);
    }

    private void triggerDeleteRendition(final QName renditionQName) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Deleting rendition " + renditionQName);
      }

      final Action deleteRenditionAction = _actionService.createAction(DeleteRenditionActionExecuter.NAME);
      deleteRenditionAction.setParameterValue(DeleteRenditionActionExecuter.PARAM_RENDITION_DEFINITION_NAME, renditionQName);
      _actionService.executeAction(deleteRenditionAction, _nodeRef);
    }
  }

}
