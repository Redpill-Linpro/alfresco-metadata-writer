package org.redpill.alfresco.module.metadatawriter.converters.impl;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.converters.ValueConverter;

import java.io.Serializable;
import java.util.StringTokenizer;


public class CategoryConverter implements ValueConverter {

	private final static Logger LOG = Logger.getLogger(CategoryConverter.class);
	
	private final NodeService nodeService;

	//---------------------------------------------------
	//Public constructor
	//---------------------------------------------------

	public CategoryConverter(
            final NodeService nodeService) {
		this.nodeService = nodeService;
		
	}
	
	//---------------------------------------------------
	//Public methods
	//---------------------------------------------------
	@Override
	public boolean applicable(final Serializable value) {
		assert value != null;
		
		LOG.warn("Testing applicability for value " + value + " of class " + value.getClass());
		
		if(value instanceof String) {
			
			final String stringValue = (String)value;
			
			//Check if this is a list of values
			if(stringValue.startsWith("[") && stringValue.endsWith("]")) {
				
				if(LOG.isDebugEnabled()) {
					LOG.debug("Found a list of values: " + value);
				}

				final String listString = stringValue.substring(1, stringValue.length()-1);
				final StringTokenizer st = new StringTokenizer(listString, ",");
				
				//It is sufficient if only one node is a category...
				while(st.hasMoreTokens()) {
					
					final String node = st.nextToken().trim(); 
					if(LOG.isDebugEnabled()) {
						LOG.debug("Is " + node + " a category?");
					}
					
					if(isCategoryNode(node)) {
						if(LOG.isDebugEnabled()) {
							LOG.debug("YES");
						}
						return true;
					}
					else {
						if(LOG.isDebugEnabled()) {
							LOG.debug("NO");
						}
					}
				}
				if(LOG.isDebugEnabled()) {
					LOG.debug("All values in list are applicable, return true");
				}
				return false;
			}
			else {
				return isCategoryNode(value);
			}
			
		}
		
		
		return false;
	}

	@Override
	public Serializable convert(final Serializable value) {
		assert value != null;
		
		if(value instanceof String) {
			
			final String stringValue = (String)value;
			
			//Check if this is a list of values
			if(stringValue.startsWith("[") && stringValue.endsWith("]")) {
				
				if(LOG.isDebugEnabled()) {
					LOG.debug("The value is a list (" + stringValue + ") converting each item...");
				}

				final StringBuilder sb = new StringBuilder();
				
				final String listString = stringValue.substring(1, stringValue.length()-1);
				final StringTokenizer st = new StringTokenizer(listString, ",");
				while(st.hasMoreTokens()) {
					final String node = st.nextToken().trim();
					if(LOG.isDebugEnabled()) {
						LOG.debug("Found node: " + node + " in list");
					}
					sb.append(getNameForNode(node));
					if(st.hasMoreTokens()) {
						sb.append(", ");
					}
					
				}
				
				if(LOG.isDebugEnabled()) {
					LOG.debug("Conversion result: " + sb.toString());
				}
				
				return sb.toString();
			}
			else {
				final Serializable result = getNameForNode(value);
				if(LOG.isDebugEnabled()) {
					LOG.debug("Conversion result: " + result);
				}
				
				return result;
			}
		}

		LOG.warn("Tried to convert " + value + ", which of class " + value.getClass());
		return null;
				
	}
	
	

	//---------------------------------------------------
	//Private methods
	//---------------------------------------------------

	private boolean isCategoryNode(final Serializable node) {
		
		if(node instanceof String && NodeRef.isNodeRef((String)node)) {
			final NodeRef nodeRef = new NodeRef((String)node);
			if(nodeService.exists(nodeRef)) {
				return nodeService.getType(nodeRef).equals(ContentModel.TYPE_CATEGORY);
			}
		}

		return false;
		
	}
	
	private Serializable getNameForNode(final Serializable node) {
		if(node instanceof String && NodeRef.isNodeRef((String)node)) {
			final NodeRef nodeRef = new NodeRef((String)node);
			if(nodeService.exists(nodeRef)) {
				return nodeService.getProperty(nodeRef, ContentModel.PROP_NAME);
			}
		}
		return "";
	}
	
}
