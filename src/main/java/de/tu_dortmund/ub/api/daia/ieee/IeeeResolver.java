package de.tu_dortmund.ub.api.daia.ieee;

import de.tu_dortmund.ub.api.daia.model.Document;

import java.util.HashMap;
import java.util.Properties;

public interface IeeeResolver {

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
