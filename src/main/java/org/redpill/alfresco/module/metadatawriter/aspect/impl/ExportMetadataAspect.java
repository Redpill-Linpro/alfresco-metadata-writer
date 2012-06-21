package org.redpill.alfresco.module.metadatawriter.aspect.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionServicePolicies.AfterCreateVersionPolicy;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataServiceRegistry;
import org.redpill.alfresco.module.metadatawriter.factories.UnknownServiceNameException;
import org.redpill.alfresco.module.metadatawriter.model.MetadataWriterModel;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService.UpdateMetadataException;
import org.springframework.beans.factory.InitializingBean;

public class ExportMetadataAspect implements AfterCreateVersionPolicy, OnUpdatePropertiesPolicy, OnAddAspectPolicy, InitializingBean {

  private static final Log LOG = LogFactory.getLog(ExportMetadataAspect.class);

  private final PolicyComponent _policyComponent;

  private final NodeService _nodeService;

  private final DictionaryService _dictionaryService;

  private final LockService _lockService;

  private final MetadataServiceRegistry _metadataServiceRegistry;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public ExportMetadataAspect(final MetadataServiceRegistry metadataServiceRegistry, final NodeService nodeService,
      final DictionaryService dictionaryService, final PolicyComponent policyComponent, final LockService lockService) {
    _metadataServiceRegistry = metadataServiceRegistry;
    _nodeService = nodeService;
    _dictionaryService = dictionaryService;
    _policyComponent = policyComponent;
    _lockService = lockService;
  }

  @Override
  public void onUpdateProperties(final NodeRef nodeRef, final Map<QName, Serializable> before, final Map<QName, Serializable> after) {
    if (!_nodeService.exists(nodeRef)) {
      return;
    }

    verifyMetadataExportableNode(nodeRef, MetadataWriterModel.ASPECT_METADATA_WRITEABLE);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Properties updated for node " + nodeRef);
    }

    // Only update properties if before and after differ
    if (_nodeService.exists(nodeRef) && !after.equals(before)) {
      updateProperties(nodeRef, after);
    }
  }

  @Override
  public void afterCreateVersion(final NodeRef versionableNode, final Version version) {
    if (!_nodeService.exists(versionableNode)) {
      return;
    }

    verifyMetadataExportableNode(versionableNode, MetadataWriterModel.ASPECT_METADATA_WRITEABLE);

    if (LOG.isDebugEnabled()) {
      LOG.debug("After create version for node " + versionableNode);
    }

    // For now only update versionLabel here
    if (_nodeService.exists(versionableNode)) {
      final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();

      properties.put(ContentModel.PROP_VERSION_LABEL, version.getVersionLabel());

      updateProperties(versionableNode, properties);
    }
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    if (!_nodeService.exists(nodeRef)) {
      return;
    }

    verifyMetadataExportableNode(nodeRef, MetadataWriterModel.ASPECT_METADATA_WRITEABLE);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Aspect updated for node " + nodeRef);
    }

    // Only update properties if before and after differ
    final Map<QName, Serializable> properties = _nodeService.getProperties(nodeRef);

    updateProperties(nodeRef, properties);
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private void verifyMetadataExportableNode(final NodeRef node, final QName aspectQName) {
    assert null != node : "Provided node is null!";
    assert _nodeService.hasAspect(node, aspectQName) : "Node " + node + " does not have mandatory aspect " + aspectQName;
  }

  private void updateProperties(final NodeRef node, final Map<QName, Serializable> properties) {
    final String serviceName = (String) _nodeService.getProperty(node, MetadataWriterModel.PROP_METADATA_SERVICE_NAME);

    final Serializable failOnUnsupportedValue = _nodeService.getProperty(node, MetadataWriterModel.PROP_METADATA_FAIL_ON_UNSUPPORTED);

    boolean failOnUnsupported = true;

    if (failOnUnsupportedValue != null) {
      failOnUnsupported = (Boolean) failOnUnsupportedValue;
    }

    try {
      _lockService.checkForLock(node);
    } catch (final NodeLockedException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node Locked! Metadata could not be exported for node " + node);
      }

      return;
    }

    if (_dictionaryService.isSubClass(_nodeService.getType(node), ContentModel.TYPE_FOLDER)) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Metadata could not be exported for folder-nodes!");
      }
    } else if (null != serviceName) {
      try {
        final MetadataService s = _metadataServiceRegistry.findService(serviceName);
        s.write(node, properties);
      } catch (final UnknownServiceNameException e) {
        LOG.warn("Could not find Metadata service named " + serviceName, e);
      } catch (final UpdateMetadataException ume) {
        if (failOnUnsupported) {
          throw new AlfrescoRuntimeException("Could not write properties " + properties + " to node "
              + _nodeService.getProperty(node, ContentModel.PROP_NAME), ume);
        } else {
          LOG.error("Could not write properties " + properties + " to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME), ume);
        }
      } catch (final Exception ex) {
        // catch the general error and log it
        LOG.error("Could not write properties " + properties + " to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME), ex);
      }
    } else {
      LOG.info("No Metadata service specified for node " + node);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    _policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this,
        "onUpdateProperties", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    _policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion"),
        MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this, "afterCreateVersion",
            Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    _policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this,
        "onAddAspect", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
  }

}
