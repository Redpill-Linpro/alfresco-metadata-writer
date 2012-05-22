package org.redpill.alfresco.module.metadatawriter.services.docx4j.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBElement;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.docx4j.docProps.core.CoreProperties;
import org.docx4j.docProps.core.dc.elements.SimpleLiteral;
import org.docx4j.docProps.custom.Properties;
import org.docx4j.docProps.custom.Properties.Property;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.io.SaveToZipFile;
import org.docx4j.openpackaging.packages.OpcPackage;
import org.docx4j.openpackaging.parts.DocPropsCorePart;
import org.docx4j.openpackaging.parts.DocPropsCustomPart;
import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.docx4j.Docx4jFacade;
import org.springframework.util.Assert;

public class Docx4jFacadeImpl implements Docx4jFacade {

  private static final Log LOG = LogFactory.getLog(Docx4jFacadeImpl.class);

  private final OutputStream _out;

  private final InputStream _in;

  private OpcPackage _opcPackage;

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  public Docx4jFacadeImpl(final InputStream in, final OutputStream out) throws IOException {
    Assert.notNull(in, "Could not create OpcPackage from null InputStream!");

    _out = out;
    _in = in;

    try {
      _opcPackage = OpcPackage.load(in);
    } catch (final Docx4JException ex) {
      throw new RuntimeException(ex);
    }
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  @Override
  public void setCustomMetadata(final String field, final String value) throws ContentException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Exporting metadata " + field + "=" + value);
    }

    final DocPropsCustomPart customPart = getDocPropsCustomPart();

    final Properties customProperties = customPart.getJaxbElement();

    Property savedProperty = null;

    // first check if the property is already present
    for (final Property property : customProperties.getProperty()) {
      if (property.getName().equalsIgnoreCase(field)) {
        savedProperty = property;
        break;
      }
    }

    if (savedProperty == null) {
      // Ok, let's add one.
      final org.docx4j.docProps.custom.ObjectFactory factory = new org.docx4j.docProps.custom.ObjectFactory();
      final org.docx4j.docProps.custom.Properties.Property newProperty = factory.createPropertiesProperty();

      newProperty.setName(field);
      newProperty.setFmtid(DocPropsCustomPart.fmtidValLpwstr); // Magic string
      newProperty.setPid(customProperties.getNextId());
      newProperty.setLpwstr(value);

      // .. add it
      customProperties.getProperty().add(newProperty);
    } else {
      savedProperty.setLpwstr(value);
    }
  }

  @Override
  public void setTitle(final String title) throws ContentException {
    final JAXBElement<SimpleLiteral> titleField = getCoreProperties().getTitle();

    if (titleField == null) {
      final org.docx4j.docProps.core.dc.elements.ObjectFactory factory = new org.docx4j.docProps.core.dc.elements.ObjectFactory();

      final SimpleLiteral literal = new SimpleLiteral();

      literal.getContent().add(title);

      getCoreProperties().setTitle(factory.createTitle(literal));
    } else {
      final List<String> content = getCoreProperties().getTitle().getValue().getContent();

      content.clear();

      content.add(title);
    }
  }

  @Override
  public void setAuthor(final String author) throws ContentException {
    final List<String> creator = getCoreProperties().getCreator().getContent();

    creator.clear();

    creator.add(author);
  }

  @Override
  public void setKeywords(final String keywords) throws ContentException {
    getCoreProperties().setKeywords(keywords);
  }

  @Override
  public void setCreateDateTime(final Date dateTime) throws ContentException {
    final List<String> created = getCoreProperties().getCreated().getContent();

    created.clear();

    // this is the proper format for this field
    final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    final String date = formatter.format(dateTime);

    created.add(date);
  }

  /**
   * Writes the updated properties to the out stream. Closes both input and
   * output when done.
   */
  @Override
  public void writeProperties() throws IOException {
    try {
      final SaveToZipFile saver = new SaveToZipFile(_opcPackage);

      saver.save(_out);
    } catch (final Docx4JException ex) {
      throw new RuntimeException(ex);
    } finally {
      closeStreams();
    }
  }

  @Override
  public void close() throws IOException {
    closeStreams();
  }

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------

  private void closeStreams() throws IOException {
    IOUtils.closeQuietly(_out);
    IOUtils.closeQuietly(_in);
  }

  private DocPropsCustomPart getDocPropsCustomPart() {
    try {
      org.docx4j.openpackaging.parts.DocPropsCustomPart docPropsCustomPart = _opcPackage.getDocPropsCustomPart();

      if (docPropsCustomPart == null) {
        docPropsCustomPart = new DocPropsCustomPart();

        docPropsCustomPart.setJaxbElement(new Properties());

        _opcPackage.addTargetPart(docPropsCustomPart);
      }

      return docPropsCustomPart;
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private CoreProperties getCoreProperties() {
    try {
      org.docx4j.openpackaging.parts.DocPropsCorePart corePart = _opcPackage.getDocPropsCorePart();

      if (corePart == null) {
        corePart = new DocPropsCorePart();

        corePart.setJaxbElement(new CoreProperties());

        _opcPackage.addTargetPart(corePart);
      }

      return corePart.getJaxbElement();
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
