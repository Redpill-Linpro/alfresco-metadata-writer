package org.redpill.alfresco.module.metadatawriter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.redpill.alfresco.module.metadatawriter.services.ContentFacade.ContentException;
import org.redpill.alfresco.module.metadatawriter.services.msoffice.impl.MSOfficeFacade;

/**
 * Application used for writing properties to an MS Office document
 * 
 * @author carl nordenfelt, redpill
 * 
 */
public class OfficeWriterApp {

  /**
   * @param args
   * @throws IOException
   * @throws FileNotFoundException
   * @throws ContentException
   */
  public static void main(final String[] args) throws FileNotFoundException, IOException, ContentException {

    assert args.length == 2 : "Must provide input and output files!";

    final String sourceFileName = args[0];
    final String targetFileName = args[1];

    final File sourceFile = new File(sourceFileName);

    final File targetFile = new File(targetFileName);

    if (!sourceFile.equals(targetFile)) {
      sourceFile.setReadOnly();
    }

    final InputStream in = new FileInputStream(sourceFile);
    // final OutputStream out = new FileOutputStream(targetFile);

    final MSOfficeFacade facade = new MSOfficeFacade(in, null);

    final Map<String, Serializable> metadata = new LinkedHashMap<String, Serializable>();

    metadata.put("Title", "t");
    metadata.put("calle", "nordenfelt");

    for (final Map.Entry<String, Serializable> m : metadata.entrySet()) {
      facade.writeMetadata(m.getKey(), m.getValue());
    }

    facade.save();

  }

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------

}
