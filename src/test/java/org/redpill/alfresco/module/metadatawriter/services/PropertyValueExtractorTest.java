package org.redpill.alfresco.module.metadatawriter.services;

import org.junit.Test;
import org.junit.Assert;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.impl.PropertyValueExtractor;

public class PropertyValueExtractorTest {
	//---------------------------------------------------
	//Public constructor
	//---------------------------------------------------

	//---------------------------------------------------
	//Public methods
	//---------------------------------------------------
	@Test
	public void nullProperty() throws ContentException {
		Assert.assertNull(PropertyValueExtractor.extractValue(null));
	}
	//---------------------------------------------------
	//Private methods
	//---------------------------------------------------

}
