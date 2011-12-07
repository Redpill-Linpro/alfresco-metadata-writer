package org.redpill.alfresco.module.metadatawriter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hpsf.NoPropertySetStreamException;
import org.apache.poi.hpsf.Property;
import org.apache.poi.hpsf.PropertySet;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.Section;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReader;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.apache.poi.util.HexDump;

public class ReaderPrototype {

  /**
   * @param args
   * @throws IOException
   * @throws FileNotFoundException
   */
  public static void main(final String[] args) throws FileNotFoundException, IOException {
    final String filename = args[0];

    final POIFSReader r = new POIFSReader();

    r.registerListener(new FullPOIFSReaderListener());

    r.read(new FileInputStream(filename));
  }

  public static class AuthorPropertyPOIFSReaderListener implements POIFSReaderListener {
    @Override
    public void processPOIFSReaderEvent(final POIFSReaderEvent event) {
      SummaryInformation si = null;
      try {
        si = (SummaryInformation) PropertySetFactory.create(event.getStream());
      } catch (final Exception ex) {
        throw new RuntimeException("Property set stream \"" + event.getPath() + event.getName() + "\": " + ex);
      }
      final String title = si.getAuthor();

      if (title != null) {
        System.out.println("Author: \"" + title + "\"");
      } else {
        System.out.println("Document has no title.");
      }

    }

  }

  private static void out(final String s) {
    System.out.println(s);
  }

  private static String hex(final byte[] bytes) {
    return HexDump.toHex(bytes);
  }

  public static class FullPOIFSReaderListener implements POIFSReaderListener {
    @Override
    public void processPOIFSReaderEvent(final POIFSReaderEvent event) {
      PropertySet ps = null;
      try {
        ps = PropertySetFactory.create(event.getStream());
      } catch (final NoPropertySetStreamException ex) {
        out("No property set stream: \"" + event.getPath() + event.getName() + "\"");
        return;
      } catch (final Exception ex) {
        throw new RuntimeException("Property set stream \"" + event.getPath() + event.getName() + "\": " + ex);
      }

      /* Print the name of the property set stream: */
      out("Property set stream \"" + event.getPath() + event.getName() + "\":");
      /* Print the number of sections: */
      final long sectionCount = ps.getSectionCount();
      out("   No. of sections: " + sectionCount);

      /* Print the list of sections: */
      @SuppressWarnings("unchecked")
      final List<Section> sections = ps.getSections();
      int nr = 0;
      for (final Section sec : sections) {
        out("   Section " + nr++ + ":");
        String s = hex(sec.getFormatID().getBytes());
        s = s.substring(0, s.length() - 1);
        out("      Format ID: " + s);

        /* Print the number of properties in this section. */
        final int propertyCount = sec.getPropertyCount();
        out("      No. of properties: " + propertyCount);

        /* Print the properties: */
        final Property[] properties = sec.getProperties();
        for (final Property p : properties) {
          /* Print a single property: */
          final long id = p.getID();
          final long type = p.getType();
          final Object value = p.getValue();
          out("      Property ID: " + id + ", type: " + type + ", value: " + value);
        }
      }
    }
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
