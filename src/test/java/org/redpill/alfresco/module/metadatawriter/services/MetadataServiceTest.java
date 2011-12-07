package org.redpill.alfresco.module.metadatawriter.services;

import java.io.IOException;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
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


public class MetadataServiceTest {
	private static final String SERVICE_NAME = "test-service";
	
	private final Mockery mockery = new Mockery();
	
	private final MetadataServiceRegistry registry = mockery.mock(MetadataServiceRegistry.class);
	private final MetadataContentFactory contentFactory = mockery.mock(MetadataContentFactory.class);
	private final ContentFacade content = mockery.mock(ContentFacade.class);
	private final NamespaceService namespaceService = mockery.mock(NamespaceService.class);
	
	private final NodeRef contentRef = new NodeRef(":///content");

	private final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
	private final Properties metadataMapping = new Properties();
	
	private final List<ValueConverter> converters = new ArrayList<ValueConverter>();
	
	
	//---------------------------------------------------
	//Setup
	//---------------------------------------------------
	@Before
	public void setUp() {
		properties.clear();
		metadataMapping.clear();
	}
	
	@After
	public void verify() {
//		mockery.assertIsSatisfied();
	}
	
	//---------------------------------------------------
	//Test
	//---------------------------------------------------
	@Test 
	public void register() {
		
		final MetadataServiceImpl s = createService();
		
		mockery.checking(new Expectations() {{
			oneOf(registry).register(s);
		}});
		
		s.register();
	}

	
	
	@Test
	public void storeEmptyProperties() throws UpdateMetadataException, ContentException, UnsupportedMimetypeException, IOException {
		expectCreateContent();
		expectNoExport();
		expectSave();
		
		createMapping("1", "a:ett");
		createMapping("2", "b:två");
		
		final MetadataServiceImpl s = createService();
		
		startTest(s);
	}
	


	@Test
	public void noMapping() throws ContentException, UpdateMetadataException, UnsupportedMimetypeException, IOException {
		
		addProperty(createQName("1"), "ett");
		addProperty(createQName("2"), "två");
		
		expectCreateContent();
		expectNoExport();
		expectSave();
		
		startTest(createService());
		
	}
	
	@Test
	public void nullProperty() throws ContentException, UpdateMetadataException, UnsupportedMimetypeException, IOException {
		
		addProperty("{a}prop", null);
		createMapping("key", "a:prop");
		
		expectCreateContent();
		expectNoExport();
		expectSave();
		
		startTest(createService());
	}

	@Test
	public void storeOneProperty() throws ContentException, UpdateMetadataException, UnsupportedMimetypeException, IOException {
		addProperty("{a}prop", "the-property-value");
		createMapping("key", "a:prop");
		

		expectCreateContent();
		expectExport("key", "the-property-value");
		expectSave();

		startTest(createService());
	}

	@Test
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
	
	@Test 
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

	//---------------------------------------------------
	//Helpers
	//---------------------------------------------------

	private void createConverter(final Serializable value, final Serializable convertedValue) {
		final ValueConverter c = mockery.mock(ValueConverter.class, "Convert " + value + "->" + convertedValue);
		converters.add(c);
		
		mockery.checking(new Expectations() {{
			allowing(c).applicable(value);
			will(returnValue(true));
			allowing(c).convert(value);
			will(returnValue(convertedValue));
		}});
		
	}

	private void addProperty(final String qNameStr, final Serializable propValue) {
		properties.put(QName.createQName(qNameStr), propValue);
	}
	
	private void createMapping(String key, final String value) {
		metadataMapping.put(key, value);
		
		mockery.checking(new Expectations() {{
			allowing(namespaceService).getNamespaceURI(value.substring(0, value.indexOf(QName.NAMESPACE_PREFIX)));
			will(returnValue(value.substring(0, value.indexOf(QName.NAMESPACE_PREFIX))));
		}});
		
	}
	
	private MetadataServiceImpl createService() {
		return new MetadataServiceImpl(registry, contentFactory, namespaceService, metadataMapping, SERVICE_NAME, converters);
	}
	
	private void startTest(MetadataServiceImpl service) throws UpdateMetadataException {
		service.write(contentRef, properties);
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
		return QName.createQName(QName.NAMESPACE_BEGIN + "namespace" +QName.NAMESPACE_END +  key);
	}

	//---------------------------------------------------
	//Mocking
	//---------------------------------------------------

	private void expectCreateContent() throws UnsupportedMimetypeException, IOException {
		mockery.checking(new Expectations() {{
			oneOf(contentFactory).createContent(contentRef);
			will(returnValue(content));
		}});
	}
	
	private void expectExport(final String field, final String value) throws ContentException {
		mockery.checking(new Expectations() {{
			oneOf(content).writeMetadata(field, value);
		}});
	}

	private void expectSave() throws ContentException {
		mockery.checking(new Expectations() {{
			oneOf(content).save();
		}});
	}

	private void expectNoExport() throws ContentException {
		mockery.checking(new Expectations() {{
			never(content).writeMetadata(with(any(String.class)), with(any(String.class)));
		}});
	}
	
}
