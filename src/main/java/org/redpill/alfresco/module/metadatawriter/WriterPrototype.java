package org.redpill.alfresco.module.metadatawriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;

import org.apache.poi.hpsf.CustomProperties;
import org.apache.poi.hpsf.DocumentSummaryInformation;
import org.apache.poi.hpsf.MarkUnsupportedException;
import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.UnexpectedPropertySetTypeException;
import org.apache.poi.hpsf.WritingNotSupportedException;
import org.apache.poi.poifs.filesystem.DirectoryEntry;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

public class WriterPrototype {

  /**
   * @param args
   */
  public static void main(final String[] args) {

    assert args.length == 1 : "File must be supplied!";

    try {

      final InputStream is = new FileInputStream(createFile(args[0]));
      final POIFSFileSystem poifs = new POIFSFileSystem(is);
      is.close();

      final DirectoryEntry dir = poifs.getRoot();
      DocumentSummaryInformation dsi;
      try {
        final DocumentEntry dsiEntry = (DocumentEntry) dir.getEntry(DocumentSummaryInformation.DEFAULT_STREAM_NAME);
        final DocumentInputStream dis = new DocumentInputStream(dsiEntry);
        final PropertySet ps = new PropertySet(dis);
        dis.close();
        dsi = new DocumentSummaryInformation(ps);
      } catch (final FileNotFoundException ex) {
        /*
         * There is no document summary information. We have to create a new
         * one.
         */
        dsi = PropertySetFactory.newDocumentSummaryInformation();
      } catch (final NoPropertySetStreamException e) {
        throw new RuntimeException(e);
      } catch (final MarkUnsupportedException e) {
        throw new RuntimeException(e);
      } catch (final UnexpectedPropertySetTypeException e) {
        throw new RuntimeException(e);
      }

      CustomProperties customProperties = dsi.getCustomProperties();
      if (customProperties == null) {
        customProperties = new CustomProperties();
      }

      /* Insert some custom properties into the container. */
      customProperties.put("Key 1", "Value 1");
      customProperties.put("Schlüssel 2", "Wert 2");
      customProperties.put("Sample Number", new Integer(12345));
      customProperties.put("Sample Boolean", new Boolean(true));
      customProperties.put("Sample Date", new Date());
      customProperties.put("sadlkjdsa", "iuiwqey");

      // Write the custom properties back to the document summary information.
      dsi.setCustomProperties(customProperties);

      try {
        dsi.write(dir, DocumentSummaryInformation.DEFAULT_STREAM_NAME);
      } catch (final WritingNotSupportedException e) {
        throw new RuntimeException(e);
      }

      final OutputStream out = new FileOutputStream(createFile(args[0]));
      poifs.writeFilesystem(out);
      out.close();

    } catch (final FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }

  }

  // ---------------------------------------------------
  // Public constructor
  // ---------------------------------------------------

  private static File createFile(final String file) throws FileNotFoundException {
    final File f = new File(file);

    if (f.exists()) {
      return f;
    }

    throw new FileNotFoundException("Could not find file " + file);
  }

  // ---------------------------------------------------
  // Public methods
  // ---------------------------------------------------

  // ---------------------------------------------------
  // Private methods
  // ---------------------------------------------------

}
