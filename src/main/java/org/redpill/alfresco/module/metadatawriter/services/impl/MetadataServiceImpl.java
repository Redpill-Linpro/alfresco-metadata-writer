package org.redpill.alfresco.module.metadatawriter.services.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
import org.apache.commons.lang.StringUtils;
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class MetadataServiceImpl implements MetadataService, InitializingBean, DisposableBean {

  private static final Logger LOG = Logger.getLogger(MetadataServiceImpl.class);

  private Map<QName, String> _metadataMapping;
  private MetadataServiceRegistry _metadataServiceRegistry;
  private MetadataContentFactory _metadataContentFactory;
  private NamespaceService _namespaceService;
  private String _serviceName;
  private List<ValueConverter> _converters;
  private TransactionService _transactionService;
  private BehaviourFilter _behaviourFilter;
  private TransactionListener _transactionListener;
  private NodeService _nodeService;
  private NodeMetadataProcessor _nodeMetadataProcessor;
  private NodeVerifierProcessor _nodeVerifierProcessor;
  private ActionService _actionService;

  /**
   * Controls if existing renditions will be deleted after a successful metadata
   * write. If renditions are not deleted they may not reflect the actual node
   * content Controls if existing renditions will be deleted after a successful
   * metadata write. If renditions are not deleted they may not reflect the
   * actual node content
   */
  private boolean _deleteRenditions = false;

  private static final Object KEY_UPDATER = MetadataService.class.getName() + ".updater";
  private static final Object KEY_FAIL_SILENTLY_ON_TIMEOUT = MetadataService.class.getName() + ".failSilently";
  private ExecutorService _executorService;
  private int _timeout = MetadataService.DEFAULT_TIMEOUT;

  private boolean _failSilentlyOnTimeout = false;

  public MetadataServiceImpl() {
  }

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public MetadataServiceImpl(final MetadataServiceRegistry metadataServiceRegistry, final MetadataContentFactory metadataContentFactory, final NamespaceService namespaceService,
      TransactionService transactionService, BehaviourFilter behaviourFilter, final NodeService nodeService, final ActionService actionService) {
    _metadataServiceRegistry = metadataServiceRegistry;
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
    _metadataServiceRegistry = registry;
    _metadataContentFactory = metadataContentFactory;
    _namespaceService = namespaceService;
    _transactionService = transactionService;
    _behaviourFilter = behaviourFilter;
    _serviceName = serviceName;
    _converters = converters;
    _metadataMapping = convertMappings(mappings);
    _nodeService = nodeService;
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

  public void setTimeout(int timeout) {
    _timeout = timeout;
  }

  public void setDeleteRenditions(boolean deleteRenditions) {
    _deleteRenditions = deleteRenditions;
  }

  public void setFailSilentlyOnTimeout(String failSilentlyOnTimeout) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Fail metadatawriter silently on timeout: " + failSilentlyOnTimeout);
    }

    _failSilentlyOnTimeout = StringUtils.isBlank(failSilentlyOnTimeout) ? false : failSilentlyOnTimeout.equalsIgnoreCase("true");
  }

  public void setMetadataServiceRegistry(MetadataServiceRegistry metadataServiceRegistry) {
    _metadataServiceRegistry = metadataServiceRegistry;
  }

  public void setMetadataContentFactory(MetadataContentFactory metadataContentFactory) {
    _metadataContentFactory = metadataContentFactory;
  }

  public void setNamespaceService(NamespaceService namespaceService) {
    _namespaceService = namespaceService;
  }

  public void setTransactionService(TransactionService transactionService) {
    _transactionService = transactionService;
  }

  public void setBehaviourFilter(BehaviourFilter behaviourFilter) {
    _behaviourFilter = behaviourFilter;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setActionService(ActionService actionService) {
    _actionService = actionService;
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
    AlfrescoTransactionSupport.bindResource(KEY_FAIL_SILENTLY_ON_TIMEOUT, _failSilentlyOnTimeout);
  }

  public void register() {
    _metadataServiceRegistry.register(this);
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
    	  LOG.warn("Failed to write metadata for node " + node.toString() + ", caused by " + ume.getMessage());
          if(LOG.isDebugEnabled()) {
       	    LOG.debug("Failed to write properties " + _nodeService.getProperties(node) + " to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME) + " (" + node + ")", ume);
          }
      }
    } catch (final Exception ex) {
      
      LOG.warn("Failed to write metadata for node " + node.toString() + ", caused by " + ex.getMessage());
      if(LOG.isDebugEnabled()) {
   	    LOG.debug("Failed to write properties " + _nodeService.getProperties(node) + " to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME) + " (" + node + ")", ex);
      }
      
    } finally {
      _behaviourFilter.enableBehaviour();
    }
  }

  private class MetadataWriterTransactionListener extends TransactionListenerAdapter {

    @Override
    public void afterCommit() {
      MetadataWriterUpdater updater = AlfrescoTransactionSupport.getResource(KEY_UPDATER);
      boolean failSilentlyOnTimeout = AlfrescoTransactionSupport.getResource(KEY_FAIL_SILENTLY_ON_TIMEOUT);

      if (updater == null) {
        throw new Error("MetadataWriterUpdater was null after commit!");
      }

      FutureTask<Void> task = null;
      try {
        task = new FutureTask<Void>(updater);

        _executorService.execute(task);

        task.get(_timeout, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        task.cancel(true);

        LOG.warn(e.getMessage(), e);

        if (!failSilentlyOnTimeout) {
          throw new RuntimeException(e);
        }
      } catch (InterruptedException e) {
        // We were asked to stop
        task.cancel(true);

        return;
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private class MetadataWriterUpdater implements Callable<Void> {

    private final NodeRef _nodeRef;
    private final MetadataWriterCallback _callback;

    public MetadataWriterUpdater(final NodeRef nodeRef, final MetadataWriterCallback callback) {
      _nodeRef = nodeRef;
      _callback = callback;
    }

    @Override
    public Void call() throws Exception {
      AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>() {

        @Override
        public Void doWork() throws Exception {
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

          return null;
        }

      });

      if (_deleteRenditions) {
        deleteRenditions();
      }

      if (_callback != null) {
        _callback.execute();
      }

      return null;
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

  @Override
  public void afterPropertiesSet() throws Exception {
    _executorService = Executors.newCachedThreadPool();
  }

  @Override
  public void destroy() throws Exception {
    _executorService.shutdown();
  }

}
