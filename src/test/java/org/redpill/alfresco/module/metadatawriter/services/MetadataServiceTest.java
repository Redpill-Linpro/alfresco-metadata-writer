package org.redpill.alfresco.module.metadatawriter.services;

import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.converters.ValueConverter;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataServiceRegistry;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService.UpdateMetadataException;
import org.redpill.alfresco.module.metadatawriter.services.impl.MetadataServiceImpl;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.*;

public class MetadataServiceTest {
  private static final String SERVICE_NAME = "test-service";

  private Mockery mockery = new Mockery();

  private MetadataServiceRegistry registry = mockery.mock(MetadataServiceRegistry.class);
  private MetadataContentFactory contentFactory = mockery.mock(MetadataContentFactory.class);
  private ContentFacade content = mockery.mock(ContentFacade.class);
  private NamespaceService namespaceService = mockery.mock(NamespaceService.class);
  private TransactionService transactionService = null;
  private BehaviourFilter behaviourFilter = mockery.mock(BehaviourFilter.class);
  private NodeService nodeService = mockery.mock(NodeService.class);
  private ActionService actionService = mockery.mock(ActionService.class);

  private NodeRef contentRef = new NodeRef(":///content");

  private Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
  private Properties metadataMapping = new Properties();

  private List<ValueConverter> converters = new ArrayList<ValueConverter>();

  // ---------------------------------------------------
  // Setup
  // ---------------------------------------------------
  @Before
  public void setUp() {
    properties.clear();
    metadataMapping.clear();
  }

  @After
  public void verify() {
    // mockery.assertIsSatisfied();
  }

  // ---------------------------------------------------
  // Test
  // ---------------------------------------------------
  @Test
  public void register() {

    final MetadataServiceImpl s = createService();

    mockery.checking(new Expectations() {
      {
        oneOf(registry).register(s);
      }
    });

    s.postConstruct();
  }

  // @Test
  public void storeEmptyProperties() throws UpdateMetadataException, ContentException, UnsupportedMimetypeException, IOException {
    expectCreateContent();
    expectNoExport();
    expectSave();

    createMapping("1", "a:ett");
    createMapping("2", "b:två");

    final MetadataServiceImpl s = createService();

    startTest(s);
  }

  // @Test
  public void noMapping() throws ContentException, UpdateMetadataException, UnsupportedMimetypeException, IOException {

    addProperty(createQName("1"), "ett");
    addProperty(createQName("2"), "två");

    expectCreateContent();
    expectNoExport();
    expectSave();

    startTest(createService());

  }

  // @Test
  public void nullProperty() throws ContentException, UpdateMetadataException, UnsupportedMimetypeException, IOException {

    addProperty("{a}prop", null);
    createMapping("key", "a:prop");

    expectCreateContent();
    expectNoExport();
    expectSave();

    startTest(createService());
  }

  // @Test
  public void storeOneProperty() throws ContentException, UpdateMetadataException, UnsupportedMimetypeException, IOException {
    addProperty("{a}prop", "the-property-value");
    createMapping("key", "a:prop");

    expectCreateContent();
    expectExport("key", "the-property-value");
    expectSave();

    startTest(createService());
  }

  // @Test
  public void storeMultipleProperties() throws ContentException, UpdateMetadataException, UnsupportedMimetypeException, IOException {
    addPropertyAndMapping("one", "a:value1");
    addPropertyAndMapping("two", "a:value2");

    addProperty("{a}prop", "value1");
    createMapping("one", "a:prop");

    addProperty("{b}prop", "value2");
    createMapping("two", "b:prop");

    addProperty("{b}three", "value3");
    createMapping("four", "c:four");

    expectCreateContent();
    expectExport("one", "value1");
    expectExport("two", "value2");
    expectSave();

    startTest(createService());
  }

  // @Test
  public void storeConvertedValue() throws UnsupportedMimetypeException, IOException, ContentException, UpdateMetadataException {

    final Date v = new Date();
    final String convertedValue = new SimpleDateFormat("yyyy-MM-dd").format(v);

    addPropertyAndMapping("one", "a:value1");
    addProperty("{a}prop", v);
    createMapping("one", "a:prop");

    createConverter(v, convertedValue);

    expectCreateContent();
    expectExport("one", convertedValue);
    expectSave();

    startTest(createService());
  }

  // ---------------------------------------------------
  // Helpers
  // ---------------------------------------------------

  private void createConverter(final Serializable value, final Serializable convertedValue) {
    final ValueConverter c = mockery.mock(ValueConverter.class, "Convert " + value + "->" + convertedValue);
    converters.add(c);

    mockery.checking(new Expectations() {
      {
        allowing(c).applicable(value);
        will(returnValue(true));
        allowing(c).convert(value);
        will(returnValue(convertedValue));
      }
    });

  }

  private void addProperty(final String qNameStr, final Serializable propValue) {
    properties.put(QName.createQName(qNameStr), propValue);
  }

  private void createMapping(String key, final String value) {
    metadataMapping.put(key, value);

    mockery.checking(new Expectations() {
      {
        allowing(namespaceService).getNamespaceURI(value.substring(0, value.indexOf(QName.NAMESPACE_PREFIX)));
        will(returnValue(value.substring(0, value.indexOf(QName.NAMESPACE_PREFIX))));
      }
    });

  }

  private MetadataServiceImpl createService() {
    MetadataServiceImpl metadataService = new MetadataServiceImpl() {

      @Override
      public String getServiceName() {
        return SERVICE_NAME;
      }

    };

    metadataService.setActionService(actionService);
    metadataService.setBehaviourFilter(behaviourFilter);
    metadataService.setConverters(converters);
    metadataService.setMappings(metadataMapping);
    metadataService.setMetadataContentFactory(contentFactory);
    metadataService.setMetadataServiceRegistry(registry);
    metadataService.setNamespaceService(namespaceService);
    metadataService.setNodeService(nodeService);
    metadataService.setTransactionService(transactionService);
    
    return metadataService;
  }

  private void startTest(MetadataServiceImpl service) throws UpdateMetadataException {
    service.write(contentRef);
  }

  private void addPropertyAndMapping(String key, String value) {
    final QName qName = createQName(key);
    addProperty(qName, value);
    createMapping(key, value);
  }

  private void addProperty(QName key, String value) {
    properties.put(key, value);
  }

  private QName createQName(String key) {
    return QName.createQName(QName.NAMESPACE_BEGIN + "namespace" + QName.NAMESPACE_END + key);
  }

  // ---------------------------------------------------
  // Mocking
  // ---------------------------------------------------

  private void expectCreateContent() throws UnsupportedMimetypeException, IOException {
    mockery.checking(new Expectations() {
      {
        oneOf(contentFactory).createContent(contentRef);
        will(returnValue(content));
      }
    });
  }

  private void expectExport(final String field, final String value) throws ContentException {
    mockery.checking(new Expectations() {
      {
        oneOf(content).writeMetadata(field, value);
      }
    });
  }

  private void expectSave() throws ContentException {
    mockery.checking(new Expectations() {
      {
        oneOf(content).save();
      }
    });
  }

  private void expectNoExport() throws ContentException {
    mockery.checking(new Expectations() {
      {
        never(content).writeMetadata(with(any(String.class)), with(any(String.class)));
      }
    });
  }

}
