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
