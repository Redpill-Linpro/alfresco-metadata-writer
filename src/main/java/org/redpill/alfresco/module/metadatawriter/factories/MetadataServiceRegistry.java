package org.redpill.alfresco.module.metadatawriter.factories;

import org.redpill.alfresco.module.metadatawriter.services.MetadataService;

public interface MetadataServiceRegistry {

  MetadataService findService(String serviceName) throws UnknownServiceNameException;

  void register(MetadataService service);

}
