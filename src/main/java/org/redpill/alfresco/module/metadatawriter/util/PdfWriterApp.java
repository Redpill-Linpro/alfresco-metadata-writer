package org.redpill.alfresco.module.metadatawriter.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl.PdfboxFacade;

public class PdfWriterApp {

  public static void main(final String[] args) {
    assert args.length == 2 : "Must provide input and output files!";

    final String sourceFileName = args[0];
    final String targetFileName = args[1];

    final File sourceFile = new File(sourceFileName);

    final File targetFile = new File(targetFileName);

    if (!sourceFile.equals(targetFile)) {
      sourceFile.setReadOnly();
    }

    InputStream in = null;
    OutputStream out = null;

    try {
      in = new FileInputStream(sourceFile);
      out = new FileOutputStream(targetFile);

      final PdfboxFacade facade = new PdfboxFacade(in, out);

      final Map<String, Serializable> metadata = new LinkedHashMap<String, Serializable>();

      metadata.put("Title", "this is a title");
      metadata.put("Niklas", "Ekman");
      metadata.put("DC:identifier.documentid", "this is an id");

      for (final Map.Entry<String, Serializable> m : metadata.entrySet()) {
        facade.writeMetadata(m.getKey(), m.getValue());
      }

      facade.save();
    } catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
  }

}
