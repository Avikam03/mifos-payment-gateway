/**
 * File: CollectionNotification.java
 * ===========================================
 * This class represents the notification sends
 * whenever a collection hits your beyonic account
 * @author vladimir fomene
 */



package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionNotification {
    @JsonProperty("hook")
    private Hook hook;

    @JsonProperty("data")
    private Collection data;
}
