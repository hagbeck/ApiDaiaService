/*
The MIT License (MIT)

Copyright (c) 2015, Hans-Georg Becker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package de.tu_dortmund.ub.api.daia.ils;

import de.tu_dortmund.ub.api.daia.model.Limitation;
import de.tu_dortmund.ub.api.daia.ils.model.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * @author Hans-Georg Becker
 * @version 1.0 (2014-01-17)
 */
public interface IntegratedLibrarySystem {

    /**
     *
     * @param properties
     */
    void init(Properties properties);

    HashMap<String,String> health(Properties properties);

    /**
     * Method return for a specified ID the records of the ILS catalog.
     *
     * Workflow:
     * - search title records in the ils for the given id
     * - if count(records) > 0
     *   + retrieve item records for the results
     *   + if count(items) == 0
     *     o search for acquisition items
     *
     * @param idtype Type of the ID; one of ilsid, isbn, issn, verbundid, zdbid, eki
     * @param localpart The ID
     * @return List of DAIA like documents
     * @throws ILSException
     */
    ArrayList<Document> items(String idtype, String localpart) throws ILSException;

    /**
     *
     * @param idtype
     * @param localpart
     * @return
     * @throws ILSException
     */
    ArrayList<Limitation> issues(String idtype, String localpart) throws ILSException;
}
