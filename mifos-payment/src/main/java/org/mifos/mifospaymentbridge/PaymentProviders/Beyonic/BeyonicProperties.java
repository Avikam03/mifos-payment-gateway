package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;

import org.mifos.mifospaymentbridge.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class BeyonicProperties {
    //Create service to interact with our config database
    @Autowired
    private ConfigurationService providerConfig;


    public BeyonicProperties() {
    }

    /**
     * return beyonic api endpoint
     * @return END_POINT
     */
    public String getEND_POINT() {
        return providerConfig.findOne(1L).getConfigValue();
    }

    /**
     * return beyonic api token
     * @return API_TOKEN
     */
    public String getAPI_TOKEN() {
        return providerConfig.findOne(2L).getConfigValue();
    }
}
