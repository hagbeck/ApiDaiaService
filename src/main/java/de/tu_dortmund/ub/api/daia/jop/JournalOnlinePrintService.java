package de.tu_dortmund.ub.api.daia.jop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public interface JournalOnlinePrintService {

    /**
     *
     * @param properties
     */
    void init(Properties properties);

    HashMap<String,String> health(Properties properties);

    /**
     *
     *
     * @param idtype
     * @param localpart
     * @return
     * @throws de.tu_dortmund.ub.api.daia.jop.JOPException
     */
    ArrayList<de.tu_dortmund.ub.api.daia.jop.model.Document> items(String idtype, String localpart) throws JOPException;

    /**
     *
     *
     * @param idtype
     * @param localpart
     * @return
     * @throws de.tu_dortmund.ub.api.daia.jop.JOPException
     */
    ArrayList<de.tu_dortmund.ub.api.daia.jop.model.Document> eonly(String idtype, String localpart) throws JOPException;
}
