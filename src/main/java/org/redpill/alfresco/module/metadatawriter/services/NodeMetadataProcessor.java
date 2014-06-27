package org.redpill.alfresco.module.metadatawriter.services;


import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import java.io.Serializable;
import java.util.Map;

public interface NodeMetadataProcessor {

    Map<QName, Serializable> processNode(NodeRef nodeRef);
}
