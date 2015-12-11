// SAX parser factory.
// No warranty; no copyright -- use this as you will.
// $Id: ParserFactory.java,v 1.5 1998/05/01 20:58:23 david Exp $

package org.xml.sax.helpers;

import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.ClassCastException;

import org.xml.sax.Parser;


/**
  * Java-specific class for dynamically loading SAX parsers.
  *
  * <p>This class is not part of the platform-independent definition
  * of SAX; it is an additional convenience class designed
  * specifically for Java XML application writers.  SAX applications
  * can use the static methods in this class to allocate a SAX parser
  * dynamically at run-time based either on the value of the
  * `org.xml.sax.parser' system property or on a string containing the class
  * name.</p>
  *
  * <p>Note that the application still requires an XML parser that
  * implements SAX.</p>
  *
  * @author David Megginson (ak117@freenet.carleton.ca)
  * @version 1.0
  * @see org.xml.sax.Parser
  * @see java.lang.Class
  */
public class ParserFactory {


  /**
    * Private null constructor.
  private ParserFactor ()
  {
  }


  /**
    * Create a new SAX parser using the `org.xml.sax.parser' system property.
    *
    * <p>The named class must exist and must implement the
    * org.xml.sax.Parser interface.</p>
    *
    * @exception java.lang.NullPointerException There is no value
    *            for the `org.xml.sax.parser' system property.
    * @exception java.lang.ClassNotFoundException The SAX parser
    *            class was not found (check your CLASSPATH).
    * @exception IllegalAccessException The SAX parser class was
    *            found, but you do not have permission to load
    *            it.
    * @exception InstantiationException The SAX parser class was
    *            found but could not be instantiated.
    * @exception java.lang.ClassCastException The SAX parser class
    *            was found and instantiated, but does not implement
    *            org.xml.sax.Parser.
    * @see #makeParser(java.lang.String)
    * @see org.xml.sax.Parser
    */
  public static Parser makeParser ()
    throws ClassNotFoundException,
           IllegalAccessException, 
           InstantiationException,
           NullPointerException,
           ClassCastException
  {
    String className = System.getProperty("org.xml.sax.parser");
    if (className == null) {
      throw new NullPointerException("No value for sax.parser property");
    } else {
      return makeParser(className);
    }
  }


  /**
    * Create a new SAX parser object using the class name provided.
    *
    * <p>The named class must exist and must implement the
    * org.xml.sax.Parser interface.</p>
    *
    * @param className A string containing the name of the
    *                  SAX parser class.
    * @exception java.lang.ClassNotFoundException The SAX parser
    *            class was not found (check your CLASSPATH).
    * @exception IllegalAccessException The SAX parser class was
    *            found, but you do not have permission to load
    *            it.
    * @exception InstantiationException The SAX parser class was
    *            found but could not be instantiated.
    * @exception java.lang.ClassCastException The SAX parser class
    *            was found and instantiated, but does not implement
    *            org.xml.sax.Parser.
    * @see #makeParser()
    * @see org.xml.sax.Parser
    */
  public static Parser makeParser (String className)
    throws ClassNotFoundException,
           IllegalAccessException, 
           InstantiationException,
           ClassCastException
  {
    return (Parser)(Class.forName(className).newInstance());
  }

}

