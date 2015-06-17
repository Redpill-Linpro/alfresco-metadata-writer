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
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.version.VersionServicePolicies.AfterCreateVersionPolicy;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockType;
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
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class ExportMetadataAspect implements AfterCreateVersionPolicy, OnUpdatePropertiesPolicy, OnAddAspectPolicy, InitializingBean {

  private static final Log LOG = LogFactory.getLog(ExportMetadataAspect.class);

  private PolicyComponent _policyComponent;

  private NodeService _nodeService;

  private MetadataServiceRegistry _metadataServiceRegistry;

  private LockService _lockService;

  private boolean runAsSystem;

  public void setPolicyComponent(PolicyComponent policyComponent) {
    _policyComponent = policyComponent;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setMetadataServiceRegistry(MetadataServiceRegistry metadataServiceRegistry) {
    _metadataServiceRegistry = metadataServiceRegistry;
  }

  public void setLockService(LockService lockService) {
    _lockService = lockService;
  }

  public boolean getRunAs() {
    return runAsSystem;
  }

  /*
   * public void setRunAsSystem(boolean runAsSystem) { this.runAsSystem =
   * runAsSystem; }
   */

  public void setRunAsSystem(String runAsSystem) {
    if (runAsSystem != null && runAsSystem.equalsIgnoreCase("true")) {
      this.runAsSystem = true;
    } else {
      this.runAsSystem = false;
    }
    if (LOG.isInfoEnabled()) {
      LOG.info("Run metadatawriter as System user: " + this.runAsSystem);
    }
  }

  /**
   * Internal method @see onUpdateProperties
   */
  protected void _onUpdateProperties(final NodeRef nodeRef, final Map<QName, Serializable> before, final Map<QName, Serializable> after) {
    if (!_nodeService.exists(nodeRef)) {
      return;
    }

    if (_nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY)) {
      return;
    }

    if (_nodeService.hasAspect(nodeRef, ContentModel.ASPECT_CHECKED_OUT)) {
      return;
    }

    LockType lockType = _lockService.getLockType(nodeRef);

    if (LockType.READ_ONLY_LOCK == lockType || LockType.WRITE_LOCK == lockType) {
      return;
    }

    if (!metadataExportableNode(nodeRef, MetadataWriterModel.ASPECT_METADATA_WRITEABLE)) {
      LOG.warn("Node is not eligable for metadata export");
      return;
    }

    // Only update properties if before and after differ
    if (propertiesUpdatedRequireExport(before, after)) {
      prepareWrite(nodeRef);
    } else {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Property updates did not require metadata export for node " + nodeRef);
      }
    }
  }

  @Override
  public void onUpdateProperties(final NodeRef nodeRef, final Map<QName, Serializable> before, final Map<QName, Serializable> after) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("onUpdateProperties for node " + nodeRef);
    }

    if (LOG.isTraceEnabled() && before != null && after != null) {
      LOG.trace("Before: " + before.toString());
      LOG.trace("After: " + after.toString());
    }

    if (runAsSystem) {
      AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
        @Override
        public Void doWork() throws Exception {
          _onUpdateProperties(nodeRef, before, after);
          return null;
        }
      });
    } else {
      _onUpdateProperties(nodeRef, before, after);
    }
  }

  /**
   * Internal method @see afterCreateVersion
   */
  protected void _afterCreateVersion(final NodeRef versionableNode, final Version version) {
    if (!_nodeService.exists(versionableNode)) {
      return;
    }

    if (_nodeService.hasAspect(versionableNode, ContentModel.ASPECT_WORKING_COPY)) {
      return;
    }

    if (_nodeService.hasAspect(versionableNode, ContentModel.ASPECT_CHECKED_OUT)) {
      return;
    }

    LockType lockType = _lockService.getLockType(versionableNode);

    if (LockType.READ_ONLY_LOCK == lockType || LockType.WRITE_LOCK == lockType) {
      return;
    }

    if (!metadataExportableNode(versionableNode, MetadataWriterModel.ASPECT_METADATA_WRITEABLE)) {
      LOG.warn("Node is not eligable for metadata export");
      return;
    }

    prepareWrite(versionableNode);
  }

  /**
   * After create version is needed to catch the "new" version label set.
   */
  @Override
  public void afterCreateVersion(final NodeRef versionableNode, final Version version) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("afterCreateVersion " + version.getVersionLabel() + " for node " + versionableNode);
    }
    if (runAsSystem) {
      AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
        @Override
        public Void doWork() throws Exception {
          _afterCreateVersion(versionableNode, version);
          return null;
        }
      });
    } else {
      _afterCreateVersion(versionableNode, version);
    }

  }

  /**
   * Internal method @see afterCreateVersion
   */
  protected void _onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    if (!_nodeService.exists(nodeRef)) {
      return;
    }

    if (_nodeService.hasAspect(nodeRef, ContentModel.ASPECT_WORKING_COPY)) {
      return;
    }

    if (_nodeService.hasAspect(nodeRef, ContentModel.ASPECT_CHECKED_OUT)) {
      return;
    }

    LockType lockType = _lockService.getLockType(nodeRef);

    if (LockType.READ_ONLY_LOCK == lockType || LockType.WRITE_LOCK == lockType) {
      return;
    }

    if (!metadataExportableNode(nodeRef, MetadataWriterModel.ASPECT_METADATA_WRITEABLE)) {
      LOG.warn("Node is not eligable for metadata export");
      return;
    }

    prepareWrite(nodeRef);
  }

  @Override
  public void onAddAspect(final NodeRef nodeRef, final QName aspectTypeQName) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("onAddAspect " + aspectTypeQName.toPrefixString() + " for node " + nodeRef);
    }

    if (runAsSystem) {
      AuthenticationUtil.runAsSystem(new RunAsWork<Void>() {
        @Override
        public Void doWork() throws Exception {
          _onAddAspect(nodeRef, aspectTypeQName);
          return null;
        }
      });
    } else {
      _onAddAspect(nodeRef, aspectTypeQName);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    _policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this, "onUpdateProperties",
        Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    _policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this, "onAddAspect",
        Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

    _policyComponent.bindClassBehaviour(QName.createQName(NamespaceService.ALFRESCO_URI, "afterCreateVersion"), MetadataWriterModel.ASPECT_METADATA_WRITEABLE, new JavaBehaviour(this,
        "afterCreateVersion", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private boolean metadataExportableNode(final NodeRef node, final QName aspectQName) {
    if (node==null || !_nodeService.exists(node)) {
      return false;
    } else if (!_nodeService.hasAspect(node, aspectQName)) {
      return false;
    }
    return true;
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
      e.printStackTrace(); // To change body of catch statement use File |
                           // Settings | File Templates.
    } catch (final MetadataService.UpdateMetadataException ume) {
      throw new AlfrescoRuntimeException("Could not write properties to node " + _nodeService.getProperty(node, ContentModel.PROP_NAME), ume);
    } catch (Throwable t) {
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
    // Blacklist
    final Map<QName, Serializable> beforeFiltered = new HashMap<QName, Serializable>();
    final Map<QName, Serializable> afterFiltered = new HashMap<QName, Serializable>();
    // TODO add option to add blacklisted properties by config
    beforeFiltered.putAll(before);
    afterFiltered.putAll(after);

    // Blacklist thumbnail modification data property. This will otherwise cause
    // uneccesary writes to content (observed when running metadatawrites as
    // system, this will trigger errors on thumbnail generation)

    // do this check in order for the code to backwards compatible with 4.1.x
    if (ReflectionUtils.findField(ContentModel.class, "PROP_LAST_THUMBNAIL_MODIFICATION_DATA") != null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Blacklisting property " + ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA + " from property updated comparison");
      }
      
      beforeFiltered.remove(ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA);
      afterFiltered.remove(ContentModel.PROP_LAST_THUMBNAIL_MODIFICATION_DATA);
    }

    return !beforeFiltered.equals(afterFiltered);
  }

}
