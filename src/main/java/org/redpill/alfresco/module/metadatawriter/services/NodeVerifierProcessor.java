package org.redpill.alfresco.module.metadatawriter.services;


import org.alfresco.service.cmr.repository.NodeRef;

public interface NodeVerifierProcessor {

    boolean verifyDocument(final NodeRef node);

}
