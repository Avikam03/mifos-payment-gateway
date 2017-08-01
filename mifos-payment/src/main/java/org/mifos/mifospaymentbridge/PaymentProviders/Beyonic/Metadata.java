/**
 * File: Metadata.java
 * ============================
 * This is a java representation of the
 * Metadata object in the payment object.
 * @author vladimir fomene
 */

package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Metadata {

    private Long id;

    private String name;
}
