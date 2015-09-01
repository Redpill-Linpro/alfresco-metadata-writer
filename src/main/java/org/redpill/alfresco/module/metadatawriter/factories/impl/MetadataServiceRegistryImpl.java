package org.redpill.alfresco.module.metadatawriter.factories.impl;

import java.util.HashSet;
import java.util.Set;

import org.redpill.alfresco.module.metadatawriter.factories.MetadataServiceRegistry;
import org.redpill.alfresco.module.metadatawriter.factories.UnknownServiceNameException;
import org.redpill.alfresco.module.metadatawriter.services.MetadataService;
import org.springframework.stereotype.Component;

@Component("metadata-writer.serviceRegistry")
public class MetadataServiceRegistryImpl implements MetadataServiceRegistry {

  private final Set<MetadataService> _services = new HashSet<MetadataService>();

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public MetadataServiceRegistryImpl() {
    // Empty
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  @Override
  public MetadataService findService(final String serviceName) throws UnknownServiceNameException {

    assert serviceName != null : "Must provide non null serviceName!";

    for (final MetadataService s : _services) {
      if (serviceName.equals(s.getServiceName())) {
        return s;
      }
    }

    throw new UnknownServiceNameException("Could not find any metadata service named " + serviceName + " among " + describeAvailableServices());

  }

  @Override
  public void register(final MetadataService service) {
    assert service != null : "Will not register null service!";

    _services.add(service);
  }

  @Override
  public String toString() {
    return super.toString() + " " + describeAvailableServices();
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------
  private String describeAvailableServices() {
    final StringBuilder sb = new StringBuilder("Available services:");

    for (final MetadataService service : _services) {
      sb.append(" [" + service.toString() + "]");
    }

    return sb.toString();
  }

}
