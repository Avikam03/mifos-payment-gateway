/**
 * File: Collection.java
 * ========================================
 * This class represents beyonic's collection
 * object.
 * @author vladimir fomene
 */

package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.joda.time.DateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Collection {
    private Long id;

    private Long remote_transaction_id;

    private Long organization;

    private Double amount;

    private String currency;

    private String phonenumber;

    private DateTime payment_date;

    private String reference;

    private BeyonicPaymentState status;

    private DateTime created;

    private String author;

    private DateTime modified;

    private String updated_by;

    private String collection_request;
}
