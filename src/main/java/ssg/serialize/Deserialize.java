/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ssg.serialize;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 
 * @author 000ssg
 */
public interface Deserialize {

    /**
     * Loads object form bytes.
     *
     * @param data
     * @return
     * @throws IOException
     */
    Object fromBytes(byte[] data) throws IOException;

    /**
     * Loads object from input stream.
     *
     * @param is
     * @return
     * @throws IOException
     */
    Object fromStream(InputStream is) throws IOException;

    /**
     * Converts chars sequence to an object (deserializes it).
     *
     * @param text
     * @return
     * @throws IOException
     */
    Object fromText(String text) throws IOException;

    /**
     * Loads object from URL's input stream. If complex actions are needed to
     * establish connection then use fromStream.
     *
     * @param url
     * @return
     * @throws IOException
     */
    Object fromURL(URL url) throws IOException;

    /**
     * Read object form input stream.
     *
     * @param is
     * @return
     * @throws IOException
     */
    Object read(InputStream is) throws IOException;
    
}
