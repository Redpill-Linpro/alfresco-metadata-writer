package org.redpill.alfresco.module.metadatawriter.services.impl;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.LockType;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.services.NodeVerifierProcessor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

public class DefaultVerifierProcessor implements NodeVerifierProcessor, InitializingBean {

  private static final Logger LOG = Logger.getLogger(DefaultVerifierProcessor.class);

  protected NodeService _nodeService;
  protected LockService _lockService;
  protected DictionaryService _dictionaryService;
  protected ContentService _contentService;
  protected long docxMaxSize = 0L;
  protected long xlsxMaxSize = 0L;
  protected long pptxMaxSize = 0L;

  public DefaultVerifierProcessor(NodeService nodeService, LockService lockService, DictionaryService dictionaryService, ContentService contentService) {
    _nodeService = nodeService;
    _lockService = lockService;
    _dictionaryService = dictionaryService;
    _contentService = contentService;
  }

  public void setDocxMaxSize(long docxMaxSize) {
    this.docxMaxSize = docxMaxSize;
  }

  public void setXlsxMaxSize(long xlsxMaxSize) {
    this.xlsxMaxSize = xlsxMaxSize;
  }

  public void setPptxMaxSize(long pptxMaxSize) {
    this.pptxMaxSize = pptxMaxSize;
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

    // Check for OOXML formats size limit

    ContentReader reader = _contentService.getReader(node, ContentModel.PROP_CONTENT);
    if (reader == null) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node " + node + " has no conent");
      }
      return false;
    }
    String mimetype = reader.getMimetype();
    long size = reader.getSize();
    if (MimetypeMap.MIMETYPE_OPENXML_WORDPROCESSING.equalsIgnoreCase(mimetype) && size > docxMaxSize) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node " + node + " ignored (file size too big)");
      }
      return false;
    }
    if (MimetypeMap.MIMETYPE_OPENXML_SPREADSHEET.equalsIgnoreCase(mimetype) && size > xlsxMaxSize) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node " + node + " ignored (file size too big)");
      }
      return false;
    }
    if (MimetypeMap.MIMETYPE_OPENXML_PRESENTATION.equalsIgnoreCase(mimetype) && size > pptxMaxSize) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Node " + node + " ignored (file size too big)");
      }
      return false;
    }

    return verified;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    Assert.isTrue(docxMaxSize > 0L);
    Assert.isTrue(xlsxMaxSize > 0L);
    Assert.isTrue(pptxMaxSize > 0L);
  }

}
