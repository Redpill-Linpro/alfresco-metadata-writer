package org.redpill.alfresco.module.metadatawriter.services.impl;


import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.services.NodeVerifierProcessor;


public class DefaultVerifierProcessor implements NodeVerifierProcessor{

    private static final Logger LOG = Logger.getLogger(DefaultVerifierProcessor.class);

    protected NodeService _nodeService;
    protected LockService _lockService;
    protected DictionaryService _dictionaryService;


    public DefaultVerifierProcessor(NodeService nodeService, LockService lockService, DictionaryService dictionaryService) {
        _nodeService = nodeService;
        _lockService = lockService;
        _dictionaryService = dictionaryService;
    }


    public boolean verifyDocument(final NodeRef node) {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Starting to execute DefaultVerifierProcessor#verifyDocument");
        }

        boolean verified = true;

        // Check for locks has been removed as it won't work with vti edit-online
        // (locked document is updated)
        LockStatus lockStatus = _lockService.getLockStatus(node);
        LockType lockType = _lockService.getLockType(node);

        if ((LockStatus.LOCKED.equals(lockStatus) || LockStatus.LOCK_OWNER.equals(lockStatus)) && !LockType.WRITE_LOCK.equals(lockType)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Node " + node + " ignored (locked with status " + _lockService.getLockStatus(node) + " and type " + lockType + ")");
            }

            verified = false;
        }

        if (_dictionaryService.isSubClass(_nodeService.getType(node), ContentModel.TYPE_FOLDER)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Node " + node + " ignored (" + _nodeService.getType(node) + " is a folder-type)");
            }

            verified = false;
        }

        return verified;
    }
}
