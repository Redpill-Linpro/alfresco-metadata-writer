package org.redpill.alfresco.module.metadatawriter.converters;

import java.io.Serializable;

public interface ValueConverter {

  boolean applicable(Serializable value);

  Serializable convert(Serializable value);

}
