package org.redpill.alfresco.module.metadatawriter.aspect.impl;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.node.NodeServicePolicies.OnAddAspectPolicy;
import org.alfresco.repo.node.NodeServicePolicies.OnUpdatePropertiesPolicy;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.version.VersionServicePolicies.AfterCreateVersionPolicy;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

public class ExportMetadataAspect implements AfterCreateVersionPolicy, OnUpdatePropertiesPolicy, OnAddAspectPolicy, InitializingBean {

  private static final Log LOG = LogFactory.getLog(ExportMetadataAspect.class);

  private PolicyComponent _policyComponent;

  private NodeService _nodeService;

  private MetadataServiceRegistry _metadataServiceRegistry;

  public void setPolicyComponent(PolicyComponent policyComponent) {
    _policyComponent = policyComponent;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setMetadataServiceRegistry(MetadataServiceRegistry metadataServiceRegistry) {
    _metadataServiceRegistry = metadataServiceRegistry;
  }

  @Override
  public void onUpdateProperties(final NodeRef nodeRef, final Map<QName, Serializable> before, final Map<QName, Serializable> after) {

    if (LOG.isDebugEnabled()) {
      LOG.debug("onUpdateProperties for node " + nodeRef);
    }

    if (!_nodeService.exists(nodeRef)) {
      return;
    }

    verifyMetadataExportableNode(nodeRef, MetadataWriterModel.ASPECT_METADATA_WRITEABLE);

    // Only update properties if before and after differ
    if (propertiesUpdatedRequireExport(before, after)) {
        prepareWrite(nodeRef);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Property updates did not require metadata export for node " + nodeRef);
      }
    }
  }

  /**
   * After create version is needed to catch the "new" version label set.
   */
  @Override
  public void afterCreateVersion(final NodeRef versionableNode, final Version version) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("afterCreateVersion " + version.getVersionLabel() + " for node " + versionableNode);
    }

    if (!_nodeService.exists(versionableNode)) {
      return;
    }

    verifyMetadataExportableNode(versionableNode, MetadataWriterModel.ASPECT_METADATA_WRITEABLE);

    prepareWrite(versionableNode);

  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("onAddAspect " + aspectTypeQName.toPrefixString() + " for node " + nodeRef);
    }

    if (!_nodeService.exists(nodeRef)) {
      return;
    }

    verifyMetadataExportableNode(nodeRef, MetadataWriterModel.ASPECT_METADATA_WRITEABLE);

    prepareWrite(nodeRef);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    _policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this, "onUpdateProperties",
        Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    _policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this, "onAddAspect",
        Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    _policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion"), MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this,
        "afterCreateVersion", Behaviour.NotificationFrequency.FIRST_EVENT));


  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private void verifyMetadataExportableNode(final NodeRef node, final QName aspectQName) {
      assert null != node : "Provided node is null!";
      assert _nodeService.hasAspect(node, aspectQName) : "Node " + node + " does not have mandatory aspect " + aspectQName;
  }


  private void prepareWrite(final NodeRef node) {

      final String serviceName = (String) _nodeService.getProperty(node, MetadataWriterModel.PROP_METADATA_SERVICE_NAME);

      if (!StringUtils.hasText(serviceName)) {
          if (LOG.isInfoEnabled()) {
              LOG.info("No Metadata service specified for node " + node);
          }
          return;
      }

      final MetadataService metadataService;
      try {
          metadataService = _metadataServiceRegistry.findService(serviceName);

          metadataService.write(node);
      } catch (UnknownServiceNameException e) {
          e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } catch (final MetadataService.UpdateMetadataException ume) {
        throw new AlfrescoRuntimeException("Could not write properties to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME), ume);
      }
        catch(Throwable t) {
        LOG.warn("Problem writing metadata: " + t, t);
      }
    }

    /**
     * Ensures that metadata only exported when it has to
     *
     * @param before
     * @param after
     * @return
     */
  // TODO: Should add black list of properties ignored in comparison to avoid
  // unnecessary updates
  private boolean propertiesUpdatedRequireExport(final Map<QName, Serializable> before, final Map<QName, Serializable> after) {
    return !before.equals(after);
  }



}
