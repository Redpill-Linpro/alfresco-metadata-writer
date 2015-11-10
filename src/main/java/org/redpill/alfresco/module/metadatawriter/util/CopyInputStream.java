package org.redpill.alfresco.module.metadatawriter.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.redpill.alfresco.module.metadatawriter.services.pdfbox.impl.PdfboxFacade;

public class CopyInputStream {
  private static final Logger LOG = Logger.getLogger(CopyInputStream.class);
  private InputStream _is;
  private ByteArrayOutputStream _copy = new ByteArrayOutputStream();

  /**
     * 
     */
  public CopyInputStream(InputStream is) {
    _is = is;

    try {
      copy();
    } catch (IOException ex) {
      // do nothing
    } finally {
      LOG.trace("Closing streams");
      IOUtils.closeQuietly(_is);
    }
  }

  private int copy() throws IOException {
    int read = 0;
    int chunk = 0;
    byte[] data = new byte[256];

    while (-1 != (chunk = _is.read(data))) {
      read += data.length;
      _copy.write(data, 0, chunk);
    }

    return read;
  }

  public InputStream getCopy() {
    return (InputStream) new ByteArrayInputStream(_copy.toByteArray());
  }
}