package de.tu_dortmund.ub.api.daia.linkresolver;

import de.tu_dortmund.ub.api.daia.model.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

public interface LinkResolver {

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
    ArrayList<Document> items(String idtype, String localpart) throws LinkResolverException;

}
