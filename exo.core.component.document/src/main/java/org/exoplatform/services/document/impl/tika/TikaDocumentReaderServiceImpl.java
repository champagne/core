/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.document.impl.tika;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.parser.Parser;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.HandlerNotFoundException;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;

import java.io.InputStream;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TikaDocumentReaderServiceImpl.java 111 2008-11-11 11:11:11Z serg $
 */
public class TikaDocumentReaderServiceImpl extends DocumentReaderServiceImpl
{
   public static final String TIKA_CONFIG_PATH = "tika-configuration";

   /**
    * Tika configuration - configured from tika-conf.xml, otherwise default used.
    */
   private final TikaConfig conf;

   public TikaDocumentReaderServiceImpl(ConfigurationManager configManager, InitParams params) throws Exception
   {
      super(params);

      // get tika configuration
      if (params != null && params.getValueParam(TIKA_CONFIG_PATH) != null)
      {
         InputStream is = configManager.getInputStream(params.getValueParam(TIKA_CONFIG_PATH).getValue());
         conf = new TikaConfig(is);
      }
      else
      {
         conf = TikaConfig.getDefaultConfig();
      }
   }

   /**
    * Returns document reader by mimeType. DocumentReaders are registered only by first user call.
    * 
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.document.DocumentReaderService#getDocumentReader
    * (java.lang.String)
    */
   public DocumentReader getDocumentReader(String mimeType) throws HandlerNotFoundException
   {
      try
      {
         // first check user defined old-style and previously registered TikaDocumentReaders
         return super.getDocumentReader(mimeType);
      }
      catch (HandlerNotFoundException e)
      {
         // tika-config may contain really big amount of mimetypes, but used only few,
         // so to avoid load in memory many copies of DocumentReader, we will register it
         // only if someone need it
         synchronized (this)
         {
            //check again - previous thread may register TikaDocumenReader that we are looking
            try
            {
               return super.getDocumentReader(mimeType);
            }
            catch (HandlerNotFoundException ex)
            {
               // keep working
            }

            Parser tikaParser = conf.getParser(mimeType);
            if (tikaParser != null)
            {
               TikaDocumentReader reader = new TikaDocumentReader(tikaParser, mimeType);
               //register new document reader
               super.readers_.put(mimeType, reader);
               return reader;
            }
            else
            {
               throw new HandlerNotFoundException("No appropriate properties extractor for " + mimeType);
            }
         }
      }
   }
}