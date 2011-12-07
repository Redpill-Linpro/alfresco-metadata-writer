package org.redpill.alfresco.module.metadatawriter.factories;


import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.factories.MetadataContentFactory;
import org.redpill.alfresco.module.metadatawriter.factories.UnsupportedMimetypeException;
import org.redpill.alfresco.module.metadatawriter.factories.impl.MetadataContentFactoryImpl;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;


public class MetadataContentFactoryTest {

	private MetadataContentFactory factory;
	
	private final Mockery mockery = new Mockery();
	private ContentService contentService;
	
	private final Set<MetadataContentInstantiator> instantiators = new HashSet<MetadataContentInstantiator>();
	
	//---------------------------------------------------
	//Setup
	//---------------------------------------------------
	@Before
	public void setUp() throws Exception {
		instantiators.clear();
		contentService = mockery.mock(ContentService.class);
		factory = new MetadataContentFactoryImpl(contentService, instantiators);
	}

	@After
	public void tearDown() throws Exception {
		mockery.assertIsSatisfied();
	}

	//---------------------------------------------------
	//Tests
	//---------------------------------------------------
	@Test
	public void createOfficeContent() throws UnsupportedMimetypeException, IOException {
		final NodeRef n = createNode("http:///test");
		
		final ContentReader reader = createContentReader(MimetypeMap.MIMETYPE_WORD);
		
		stubGetContentReader(n, reader, true);
		
		final ContentWriter writer = createContentWriter();
		
		stubGetContentWriter(n, writer);
		
		final MetadataContentInstantiator expected = addInstantiator(MimetypeMap.MIMETYPE_WORD, "1");
		
		expectCreate(expected, reader, writer);
		
		factory.createContent(n); 
		
	}


	@Test
	public void createUnsupportedMimetype() throws IOException {
		final NodeRef n = createNode("http:///test");
		stubGetContentReader(n, createContentReader(MimetypeMap.MIMETYPE_ATOM), true);
		
		addInstantiator(MimetypeMap.MIMETYPE_WORD, "1");
		addInstantiator(MimetypeMap.MIMETYPE_EXCEL, "2");
		
		try {
			factory.createContent(n);
		} catch (UnsupportedMimetypeException e) {
			return;
		}
		
		Assert.fail("Expected exception due to no instantiator supporting " + MimetypeMap.MIMETYPE_ATOM);
		
	}
	

	//---------------------------------------------------
	//Helpers
	//---------------------------------------------------
	private void expectCreate(final MetadataContentInstantiator expected, final ContentReader r, final ContentWriter w) throws IOException {
		mockery.checking(new Expectations() {{
			oneOf(expected).create(r, w);
		}});
	}
	
	private MetadataContentInstantiator addInstantiator(final String mimetype, final String name) {
		final MetadataContentInstantiator i = mockery.mock(MetadataContentInstantiator.class, name);
		
		mockery.checking(new Expectations() {{
			allowing(i).supports(mimetype);
			will(returnValue(true));
		}});
		
		mockery.checking(new Expectations() {{
			allowing(i).supports(with(any(String.class)));
			will(returnValue(false));
		}});
		
		instantiators.add(i);
		
		return i;
	}

	private ContentWriter createContentWriter() {
		return mockery.mock(ContentWriter.class);
	}

	private ContentReader createContentReader(final String mimetype) {
		final ContentReader reader = mockery.mock(ContentReader.class);
		
		mockery.checking(new Expectations() {{
			allowing(reader).getMimetype();
			will(returnValue(mimetype));
		}});
		
		return reader;
	}

	private NodeRef createNode(final String name) {
		return new NodeRef(name);
	}

	private void stubGetContentReader(final NodeRef n, final ContentReader reader, final boolean readerExists) {
		mockery.checking(new Expectations() {{
			allowing(contentService).getReader(with(equal(n)), with(any(QName.class)));
			will(returnValue(reader));
			allowing(reader).exists();
			will(returnValue(readerExists));
		}});
	}
	


	private void stubGetContentWriter(final NodeRef n, final ContentWriter writer) {
		mockery.checking(new Expectations() {{
			allowing(contentService).getWriter(with(equal(n)), with(any(QName.class)), with(equal(true)));
			will(returnValue(writer));
		}});
	}


}
