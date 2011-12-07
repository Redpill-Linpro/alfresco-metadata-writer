package org.redpill.alfresco.module.metadatawriter.services.msoffice;

import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.msoffice.POIFSFacade;
import org.redpill.alfresco.module.metadatawriter.services.msoffice.impl.MSOfficeFacade;


public class MSOfficeFacadeTest {
	
	private final Mockery mockery = new Mockery();
	private final POIFSFacade poifsFacade = mockery.mock(POIFSFacade.class);
	
	private MSOfficeFacade facade = new _MSOfficeFacade(poifsFacade);
	
	//---------------------------------------------------
	//Setup
	//---------------------------------------------------
	@Before
	public void setUp() {
		
	}
	
	@After
	public void tearDown() {
//		mockery.assertIsSatisfied();
	}
	
	
	//---------------------------------------------------
	//Test
	//---------------------------------------------------
	@Test
	public void exportCustomMetadata() throws ContentException {
		final String field = "custom field";
		final String value = "my value";
		
		expectSetCustomMetadata(field, value);
		facade.writeMetadata(field, value);
		
	}
	
	@Test
	public void updateTitle() throws ContentException {
		expectSetTitle("title");
		facade.writeMetadata("Title", "title");
	}
	
	@Test
	public void updateAuthor() throws ContentException {
		expectSetAuthor("author");
		facade.writeMetadata("Author", "author");
	}
	
	@Test
	public void updateKeywords() throws ContentException {
		expectSetKeywords("keywords");
		facade.writeMetadata("Keywords", "keywords");
	}
	
	@Test
	public void save() throws ContentException, IOException {
		expectWriteFilesystem();
		facade.save();
	}
	

	//---------------------------------------------------
	//Helpers
	//---------------------------------------------------
	private void expectWriteFilesystem() throws IOException {
		mockery.checking(new Expectations() {{
			one(poifsFacade).writeProperties();
		}});
	}

	private void expectSetCustomMetadata(final String field, final String value) throws ContentException {
		mockery.checking(new Expectations() {{
			one(poifsFacade).setCustomMetadata(with(equal(field)), with(equal(value)));
		}});
	}
	
	private void expectSetKeywords(final String keywords) throws ContentException {
		mockery.checking(new Expectations() {{
			one(poifsFacade).setKeywords(with(equal(keywords)));
		}});
	}
	
	private void expectSetAuthor(final String author) throws ContentException {
		mockery.checking(new Expectations() {{
			one(poifsFacade).setAuthor(with(equal(author)));
		}});
	}

	private void expectSetTitle(final String title) throws ContentException {
		mockery.checking(new Expectations() {{
			one(poifsFacade).setTitle(with(equal(title)));
		}});
	}


	private class _MSOfficeFacade extends MSOfficeFacade {
		
		public _MSOfficeFacade(POIFSFacade poifsFacade) {
			super(poifsFacade);
		}
		
	}
	
}
