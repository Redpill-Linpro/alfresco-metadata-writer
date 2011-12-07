package org.redpill.alfresco.module.metadatawriter.aspect.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionServicePolicies;
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

public class ExportMetadataAspect implements VersionServicePolicies.AfterCreateVersionPolicy, NodeServicePolicies.OnUpdatePropertiesPolicy {

  private static final Log logger = LogFactory.getLog(ExportMetadataAspect.class);

  private final PolicyComponent policyComponent;
  private final NodeService nodeService;
  private final DictionaryService dictionaryService;
  private final LockService lockService;

  private final MetadataServiceRegistry metadataServiceRegistry;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------
  public ExportMetadataAspect(final MetadataServiceRegistry metadataServiceRegistry, final NodeService nodeService,
      final DictionaryService dictionaryService, final PolicyComponent policyComponent, final LockService lockService) {
    this.metadataServiceRegistry = metadataServiceRegistry;
    this.nodeService = nodeService;
    this.dictionaryService = dictionaryService;
    this.policyComponent = policyComponent;
    this.lockService = lockService;
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  public void init() {

    this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "onUpdateProperties"),
        MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this, "onUpdateProperties",
            Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    // this.policyComponent.bindClassBehaviour(
    // QName.createQName(NamespaceService.ALFRESCO_URI, "beforeCreateVersion"),
    // MetadataWriterModel.ASPECT_METADATA_WRITEABLE,
    // new JavaBehaviour(this, "beforeCreateVersion",
    // Behaviour.NotificationFrequency.EVERY_EVENT));

    this.policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion"),
        MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this, "afterCreateVersion", Behaviour.NotificationFrequency.FIRST_EVENT));
  }

  public void onUpdateProperties(final NodeRef nodeRef, final Map<QName, Serializable> before, final Map<QName, Serializable> after) {

    verifyMetadataExportableNode(nodeRef, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, nodeService);

    if (logger.isDebugEnabled()) {
      logger.debug("Properties updated for node " + nodeRef);
    }

    // Only update properties if before and after differ
    if (nodeService.exists(nodeRef) && !after.equals(before)) {

      updateProperties(nodeRef, after);
    }
  }

  @Override
  public void afterCreateVersion(NodeRef versionableNode, Version version) {

    verifyMetadataExportableNode(versionableNode, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, nodeService);

    if (logger.isDebugEnabled()) {
      logger.debug("After create version for node " + versionableNode);
    }

    // For now only update versionLabel here
    if (nodeService.exists(versionableNode)) {
      final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
      properties.put(ContentModel.PROP_VERSION_LABEL, version.getVersionLabel());
      updateProperties(versionableNode, properties);
    }
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private static void verifyMetadataExportableNode(final NodeRef node, final QName aspectQName, final NodeService nodeService) {
    assert null != node : "Provided node is null!";
    assert nodeService.hasAspect(node, aspectQName) : "Node " + node + " does not have mandatory aspect " + aspectQName;
  }

  private void updateProperties(final NodeRef node, final Map<QName, Serializable> properties) {

    final String serviceName = (String) nodeService.getProperty(node, MetadataWriterModel.PROP_METADATA_SERVICE_NAME);

    try {
      lockService.checkForLock(node);
    } catch (NodeLockedException e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Node Locked! Metadata could not be exported for node " + node);
      }
      return;
    }

    if (dictionaryService.isSubClass(nodeService.getType(node), ContentModel.TYPE_FOLDER)) {
      if (logger.isDebugEnabled()) {
        logger.debug("Metadata could not be exported for folder-nodes!");
      }
    } else if (null != serviceName) {

      try {
        final MetadataService s = metadataServiceRegistry.findService(serviceName);
        s.write(node, properties);

      } catch (final UnknownServiceNameException e) {
        logger.warn("Could not find Metadata service named " + serviceName, e);
      } catch (final UpdateMetadataException ume) {
        throw new AlfrescoRuntimeException("Could not write properties " + properties + " to node "
            + nodeService.getProperty(node, ContentModel.PROP_NAME), ume);
      }

    } else {
      logger.info("No Metadata service specified for node " + node);
    }

  }

}
