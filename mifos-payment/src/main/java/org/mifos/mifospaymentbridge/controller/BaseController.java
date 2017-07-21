/**
 *  File: BaseController.java
 *  ====================================
 *  This class implements all the general functionalities
 *  needed by all controllers
 *  @author Vladimir Fomene
 */

package org.mifos.mifospaymentbridge.controller;


public abstract class BaseController {

    public static final String FINERACT_API_PATH = "/api/v1";

    public static final String LOAN_URL = FINERACT_API_PATH + "/loans/{loanId}";
}
