package org.redpill.alfresco.module.metadatawriter.factories;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertNotNull;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.factories.UnknownServiceNameException;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.factories.impl.MetadataServiceRegistryImpl;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;


public class MetadataServiceRegistryTest {
	

	private MetadataServiceRegistryImpl f;
	
	private final Mockery mockery = new Mockery();

	//---------------------------------------------------
	//Setup
	//---------------------------------------------------
	@Before
	public void setUp() {
		f = new MetadataServiceRegistryImpl();
		
	}
	
	
	@After
	public void assertIsSatisfied() {
		mockery.assertIsSatisfied();
	}
	
	//---------------------------------------------------
	//Test
	//---------------------------------------------------
	@Test
	public void createServiceAvailable() throws UnsupportedMimetypeException, UnknownServiceNameException {
		
		final String serviceName = "expected service";
		final MetadataService expectedService = createAndAddServiceFor(serviceName);
		
		createAndAddServiceFor("other service");
		
		final MetadataService createdService = f.findService(serviceName);
		
		assertNotNull(createdService);
		assertSame(expectedService, createdService);
	}


	@Test(expected=UnknownServiceNameException.class)
	public void createNoSupportedServiceAvailable() throws UnknownServiceNameException {
		final String serviceName = "any service";
		
		createAndAddServiceFor("supperted service 1");
		createAndAddServiceFor("supperted service 2");
		
		f.findService(serviceName);
	}

	//---------------------------------------------------
	//Helpers
	//---------------------------------------------------
	
	private MetadataService createAndAddServiceFor(final String serviceName) {
		
		final MetadataService service = mockery.mock(MetadataService.class, serviceName);
		f.register(service);
		
		mockery.checking(new Expectations() {{
			allowing(service).getServiceName();
			will(returnValue(serviceName));
		}});
		
		return service;
	}
	



}
