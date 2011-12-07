package org.redpill.alfresco.module.metadatawriter.converters.impl;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.redpill.alfresco.module.metadatawriter.converters.ValueConverter;

public class DateConverter implements ValueConverter {

  private final SimpleDateFormat _dateFormat;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public DateConverter(final String dateFormat) {
    _dateFormat = new SimpleDateFormat(dateFormat);
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  @Override
  public boolean applicable(final Serializable value) {
    return value instanceof Date;
  }

  @Override
  public Serializable convert(final Serializable value) {
    return value != null ? _dateFormat.format(value) : null;
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------

}
