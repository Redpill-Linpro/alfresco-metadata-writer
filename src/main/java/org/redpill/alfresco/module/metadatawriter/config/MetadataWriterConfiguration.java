package org.redpill.alfresco.module.metadatawriter.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.alfresco.repo.dictionary.DictionaryBootstrap;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.repo.tenant.TenantService;
import org.redpill.alfresco.module.metadatawriter.factories.impl.MetadataContentFactoryImpl;
import org.redpill.alfresco.module.metadatawriter.services.MetadataContentInstantiator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

@Configuration
public class MetadataWriterConfiguration {

  @Autowired
  @Qualifier("metadata-writer.PoiFsInstantiator")
  private MetadataContentInstantiator _poiFsInstantiator;

  @Autowired
  @Qualifier("metadata-writer.PoixInstantiator")
  private MetadataContentInstantiator _poixFsInstantiator;

  @Autowired
  @Qualifier("metadata-writer.PdfInstantiator")
  private MetadataContentInstantiator _pdfInstantiator;

  @Autowired
  @Qualifier("metadata-writer.OdfInstantiator")
  private MetadataContentInstantiator _odfInstantiator;

  @Autowired
  private TenantService _tenantService;

  @Autowired
  private DictionaryDAO _dictionaryDAO;
  
  @Bean(name = "metadata-writer.contentFactory")
  public MetadataContentFactoryImpl foo() {
    MetadataContentFactoryImpl metadataContentFactory = new MetadataContentFactoryImpl();

    Set<MetadataContentInstantiator> instantiators = new HashSet<MetadataContentInstantiator>();
    instantiators.add(_poiFsInstantiator);
    instantiators.add(_poixFsInstantiator);
    instantiators.add(_pdfInstantiator);
    instantiators.add(_odfInstantiator);

    metadataContentFactory.setInstantiators(instantiators);

    return metadataContentFactory;
  }

  @Bean(name = "metadata-content.dictionaryBootstrap", initMethod = "bootstrap")
  @DependsOn("dictionaryBootstrap")
  public DictionaryBootstrap dictionaryBootstrap() {
    DictionaryBootstrap dictionaryBootstrap = new DictionaryBootstrap();

    List<String> modelResources = new ArrayList<String>();
    modelResources.add("alfresco/extension/model/metadataWriterModel.xml");

    dictionaryBootstrap.setTenantService(_tenantService);
    dictionaryBootstrap.setDictionaryDAO(_dictionaryDAO);
    dictionaryBootstrap.setModels(modelResources);

    return dictionaryBootstrap;
  }

}
