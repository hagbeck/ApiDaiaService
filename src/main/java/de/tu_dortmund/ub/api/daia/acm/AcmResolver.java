package de.tu_dortmund.ub.api.daia.acm;

import de.tu_dortmund.ub.api.daia.model.Document;

import java.util.HashMap;
import java.util.Properties;

public interface AcmResolver {

    /**
     *
     * @param properties
     */
    void init(Properties properties);

    /**
     *
     * @param latinParameters
     * @return
     */
    Document items(HashMap<String, String> latinParameters);
}
