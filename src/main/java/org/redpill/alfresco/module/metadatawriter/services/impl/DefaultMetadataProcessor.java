package org.redpill.alfresco.module.metadatawriter.services.impl;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.services.NodeMetadataProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Map;

@Component("metadata-writer.metadataProcessor")
public class DefaultMetadataProcessor implements NodeMetadataProcessor {

  private static final Logger LOG = Logger.getLogger(DefaultMetadataProcessor.class);

  @Autowired
  @Qualifier("NodeService")
  private NodeService _nodeService;

  @Override
  public Map<QName, Serializable> processNode(final NodeRef nodeRef) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Starting to execute DefaultMetadataProcessor#verifyDocument");
    }

    return AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Map<QName, Serializable>>() {

      @Override
      public Map<QName, Serializable> doWork() throws Exception {
        return _nodeService.getProperties(nodeRef);
      }

    });
    
  }
  
}
