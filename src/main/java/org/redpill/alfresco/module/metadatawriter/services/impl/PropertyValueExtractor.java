package org.redpill.alfresco.module.metadatawriter.services.impl;

import java.io.Serializable;
import java.util.Date;

import org.alfresco.service.cmr.repository.MLText;
import org.springframework.extensions.surf.util.I18NUtil;

//TODO: Move to converter
public class PropertyValueExtractor {
  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------
  public static Serializable extractValue(final Serializable property) {
    if (null == property) {
      return null;
    }

    if (property instanceof MLText) {
      return ((MLText) property).getClosestValue(I18NUtil.getContentLocale());
    }

    if (property instanceof String) {
      return property;
    }

    if (property instanceof Date) {
      return property;
    }

    // This is the best guess if the underlying class is not supported in any
    // other extract value method
    return property.toString();
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------

}
