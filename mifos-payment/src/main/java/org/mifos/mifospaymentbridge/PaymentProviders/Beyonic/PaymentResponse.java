/**
 * File: PaymentResponse.java
 * ============================
 * This is a java representation of the
 * payment object in the beyonic api.
 * @author vladimir fomene
 */

package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private Long id;

    private Long organization;

    private double amount;

    private Long account;

    private String currency;

    private String payment_type;

    private Metadata metadata;

    private String description;

    private String[] phone_nos;

    private String state;

    private String last_error;

    private String rejected_reason;

    private Long rejected_by;

    private String rejected_time;

    private String cancelled_reason;

    private Long cancelled_by;

    private String cancelled_time;

    private String created;

    private Long author;

    private String modified;

    private String updated_by;

    private String start_date;

    private String remote_transaction_id;

}
