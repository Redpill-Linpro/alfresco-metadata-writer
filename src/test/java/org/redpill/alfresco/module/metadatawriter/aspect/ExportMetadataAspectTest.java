package org.redpill.alfresco.module.metadatawriter.aspect;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.lock.NodeLockedException;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.namespace.QName;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.aspect.impl.ExportMetadataAspect;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataServiceRegistry;
import org.redpill.alfresco.module.metadatawriter.factories.UnknownServiceNameException;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.model.MetadataWriterModel;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService.UpdateMetadataException;
import org.springframework.extensions.surf.util.I18NUtil;

public class ExportMetadataAspectTest {

  private static String SERVICE_NAME = "service name";

  private ExportMetadataAspect aspect;

  private final Mockery mockery = new Mockery();
  private final MetadataServiceRegistry serviceRegistry = mockery.mock(MetadataServiceRegistry.class);
  private final NodeService nodeService = mockery.mock(NodeService.class);
  private final DictionaryService dictionaryService = mockery.mock(DictionaryService.class);
  private final PolicyComponent policyComponent = mockery.mock(PolicyComponent.class);
  private final LockService lockService = mockery.mock(LockService.class);
  private final MetadataService service = mockery.mock(MetadataService.class);

  private final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
  private final NodeRef nodeRef = new NodeRef("workspace://SpacesStore/content-node");

  static {
    I18NUtil.registerResourceBundle("org.redpill.alfresco.module.metadatawriter.aspect.unittest.msg");
  }

  // ---------------------------------------------------
  // Setup
  // ---------------------------------------------------
  @Before
  public void setUp() throws Exception {
    properties.clear();
    aspect = new ExportMetadataAspect();
    aspect.setDictionaryService(dictionaryService);
    aspect.setLockService(lockService);
    aspect.setMetadataServiceRegistry(serviceRegistry);
    aspect.setNodeService(nodeService);
    aspect.setPolicyComponent(policyComponent);
  }

  @After
  public void tearDown() throws Exception {
    // mockery.assertIsSatisfied();
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  @Test
  public void noExportForUnexistingNode() throws UnsupportedMimetypeException, UnknownServiceNameException, IOException, UpdateMetadataException {

    stubNodeExists(nodeRef, false);

    expectNeverFindService();
    expectNeverExportProperties();

    aspect.onUpdateProperties(nodeRef, null, properties);
  }

  @Test
  public void noUpdateForUnchangedProperties() throws UnknownServiceNameException, UpdateMetadataException {

    stubNodeExists(nodeRef, true);
    stubNodeLocked(nodeRef, false);
    expectNeverFindService();
    expectNeverExportProperties();

    aspect.onUpdateProperties(nodeRef, properties, properties);

  }

  // @Test
  public void nodeIsLocked() throws UpdateMetadataException {
    stubHasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, true);
    stubNodeExists(nodeRef, true);
    stubServiceName(nodeRef, SERVICE_NAME);
    stubNodeLocked(nodeRef, true);

    expectNeverExportProperties();

    aspect.onUpdateProperties(nodeRef, null, properties);

    mockery.assertIsSatisfied();
  }

  @Test
  public void noServiceWithSpecifiedName() throws UnknownServiceNameException, UpdateMetadataException {

    stubNodeExists(nodeRef, true);
    stubServiceName(nodeRef, SERVICE_NAME);
    stubNodeLocked(nodeRef, false);
    stubIsFolderSubType(nodeRef, false);
    throwUnknownServiceException(SERVICE_NAME);

    expectNeverExportProperties();

    aspect.onUpdateProperties(nodeRef, properties, properties);

    mockery.assertIsSatisfied();
  }

  @Test
  public void propertiesUpdated() throws UnsupportedMimetypeException, UnknownServiceNameException, IOException, UpdateMetadataException {

    stubHasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, false);
    stubNodeExists(nodeRef, true);
    stubServiceName(nodeRef, SERVICE_NAME);
    stubIsFolderSubType(nodeRef, false);
    stubNodeLocked(nodeRef, false);

    expectCreateService(SERVICE_NAME);
    expectUpdateProperties(nodeRef, properties);

    aspect.onUpdateProperties(nodeRef, properties, properties);
  }

//  @Test
//  public void afterVersionCreated() throws UnsupportedMimetypeException, UnknownServiceNameException, IOException, UpdateMetadataException {
//
//    final String VERSION_LABEL = "1.4";
//
//    final Version version = mockery.mock(Version.class);
//
//    mockery.checking(new Expectations() {
//      {
//        allowing(version).getVersionLabel();
//        will(returnValue(VERSION_LABEL));
//      }
//    });
//
//    stubHasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE, true);
//    stubNodeExists(nodeRef, true);
//    stubServiceName(nodeRef, SERVICE_NAME);
//    stubNodeLocked(nodeRef, false);
//    stubIsFolderSubType(nodeRef, false);
//    stubGetProperties(nodeRef);
//
//    expectCreateService(SERVICE_NAME);
//
//    properties.put(ContentModel.PROP_VERSION_LABEL, VERSION_LABEL);
//    expectUpdateProperties(nodeRef, properties);
//
//    aspect.afterCreateVersion(nodeRef, version);
//  }

  // ---------------------------------------------------
  // Helpers
  // ---------------------------------------------------

  private void stubNodeLocked(final NodeRef nodeRef, final boolean hasLock) {
    mockery.checking(new Expectations() {
      {
        allowing(lockService).getLockStatus(nodeRef);
        if (hasLock) {
          will(returnValue(LockStatus.LOCKED));
        }
        else {
        	will(returnValue(LockStatus.NO_LOCK));
        }
      }
    });
  }

  private void throwUnknownServiceException(final String serviceName) throws UnknownServiceNameException {
    mockery.checking(new Expectations() {
      {
        allowing(serviceRegistry).findService(serviceName);
        will(throwException(new UnknownServiceNameException("")));
      }
    });
  }

  private void stubIsFolderSubType(final NodeRef nodeRef, final boolean isFolderSubType) {

    final QName folderSubType = QName.createQName("//folderSubType");
    final QName contentType = QName.createQName("//contentType");

    mockery.checking(new Expectations() {
      {
        allowing(nodeService).getType(nodeRef);
        will(returnValue(isFolderSubType ? folderSubType : contentType));
        allowing(dictionaryService).isSubClass(folderSubType, ContentModel.TYPE_FOLDER);
        will(returnValue(true));
        allowing(dictionaryService).isSubClass(contentType, ContentModel.TYPE_FOLDER);
        will(returnValue(false));
      }
    });
  }

  private void stubServiceName(final NodeRef nodeRef, final String serviceName) {
    mockery.checking(new Expectations() {
      {
        allowing(nodeService).getProperty(nodeRef, MetadataWriterModel.PROP_METADATA_SERVICE_NAME);
        will(returnValue(serviceName));
      }
    });
  }

  private void stubGetProperties(final NodeRef nodeRef2) {
    mockery.checking(new Expectations() {
      {
        allowing(nodeService).getProperties(nodeRef);
        will(returnValue(properties));
      }
    });
  }

  private void stubHasAspect(final NodeRef nodeRef, final QName aspect, final boolean b) {
    mockery.checking(new Expectations() {
      {
        allowing(nodeService).hasAspect(nodeRef, aspect);
        will(returnValue(b));
      }
    });
  }

  private void stubNodeExists(final NodeRef nodeRef, final boolean b) {
    mockery.checking(new Expectations() {
      {
        allowing(nodeService).exists(nodeRef);
        will(returnValue(b));

      }
    });
  }

  @SuppressWarnings("unchecked")
  private void expectNeverExportProperties() throws UpdateMetadataException {
    mockery.checking(new Expectations() {
      {
        never(service).write(with(any(NodeRef.class)), with(any(Map.class)));
      }
    });
  }

  private void expectNeverFindService() throws UnknownServiceNameException {
    mockery.checking(new Expectations() {
      {
        never(serviceRegistry).findService(with(any(String.class)));
      }
    });
  }

  private void expectUpdateProperties(final NodeRef contentRef, final Map<QName, Serializable> properties) throws UpdateMetadataException {

    mockery.checking(new Expectations() {
      {
        oneOf(service).write(contentRef, properties);
      }
    });
  }

  private void expectCreateService(final String serviceName) throws UnknownServiceNameException {
    mockery.checking(new Expectations() {
      {
        oneOf(serviceRegistry).findService(with(equal(serviceName)));
        will(returnValue(service));
      }
    });
  }

}
