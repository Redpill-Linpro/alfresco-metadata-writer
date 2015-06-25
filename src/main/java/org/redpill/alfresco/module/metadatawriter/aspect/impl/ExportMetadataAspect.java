package org.redpill.alfresco.module.metadatawriter.aspect.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

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
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataServiceRegistry;
import org.redpill.alfresco.module.metadatawriter.factories.UnknownServiceNameException;
import org.redpill.alfresco.module.metadatawriter.model.MetadataWriterModel;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

@Component("metadata-writer.writeMetadataAspect")
@DependsOn("metadata-content.dictionaryBootstrap")
public class ExportMetadataAspect implements AfterCreateVersionPolicy, OnUpdatePropertiesPolicy, OnAddAspectPolicy {

  private static final Log LOG = LogFactory.getLog(ExportMetadataAspect.class);

  @Autowired
  @Qualifier("policyComponent")
  private PolicyComponent _policyComponent;

  @Autowired
  @Qualifier("NodeService")
  private NodeService _nodeService;

  @Autowired
  @Qualifier("metadata-writer.serviceRegistry")
  private MetadataServiceRegistry _metadataServiceRegistry;

  @Autowired
  @Qualifier("LockService")
  private LockService _lockService;

  @Value("${metadata-writer.runAsSystem}")
  private boolean _runAsSystem = false;

  private JavaBehaviour _onUpdateProperties;

  private JavaBehaviour _onAddAspect;

  private JavaBehaviour _afterCreateVersion;

  public void setRunAsSystem(String runAsSystem) {
    if (runAsSystem != null && runAsSystem.equalsIgnoreCase("true")) {
      _runAsSystem = true;
    } else {
      _runAsSystem = false;
    }

    if (LOG.isInfoEnabled()) {
      LOG.info("Run metadatawriter as System user: " + this._runAsSystem);
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

    if (_runAsSystem) {
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
    if (_runAsSystem) {
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

    if (_runAsSystem) {
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

  @PostConstruct
  public void postConstruct() {
    _onUpdateProperties = new JavaBehaviour(this, "onUpdateProperties", Behaviour.NotificationFrequency.TRANSACTION_COMMIT);
    _onAddAspect = new JavaBehaviour(this, "onAddAspect", Behaviour.NotificationFrequency.TRANSACTION_COMMIT);
    _afterCreateVersion = new JavaBehaviour(this, "afterCreateVersion", Behaviour.NotificationFrequency.TRANSACTION_COMMIT);

    _policyComponent.bindClassBehaviour(OnUpdatePropertiesPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, _onUpdateProperties);
    _policyComponent.bindClassBehaviour(OnAddAspectPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, _onAddAspect);
    _policyComponent.bindClassBehaviour(AfterCreateVersionPolicy.QNAME, MetadataWriterModel.ASPECT_METADATA_WRITEABLE, _afterCreateVersion);
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
    String serviceName = (String) _nodeService.getProperty(node, MetadataWriterModel.PROP_METADATA_SERVICE_NAME);

    if (!StringUtils.hasText(serviceName)) {
      if (LOG.isInfoEnabled()) {
        LOG.info("No Metadata service specified for node " + node);
      }

      return;
    }

    MetadataService metadataService;

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
    Map<QName, Serializable> beforeFiltered = new HashMap<QName, Serializable>();
    Map<QName, Serializable> afterFiltered = new HashMap<QName, Serializable>();
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

  // setters and getters only here for unit testing

  public void setMetadataServiceRegistry(MetadataServiceRegistry metadataServiceRegistry) {
    _metadataServiceRegistry = metadataServiceRegistry;
  }

  public void setNodeService(NodeService nodeService) {
    _nodeService = nodeService;
  }

  public void setPolicyComponent(PolicyComponent policyComponent) {
    _policyComponent = policyComponent;
  }

  public void setLockService(LockService lockService) {
    _lockService = lockService;
  }

  public boolean isRunAsSystem() {
    return _runAsSystem;
  }

}
