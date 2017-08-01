/**
 *  File: BaseController.java
 *  ====================================
 *  This class implements all the general functionalities
 *  needed by all controllers
 *  @author Vladimir Fomene
 */

package org.mifos.mifospaymentbridge.controller;


import org.mifos.mifospaymentbridge.Constant.GatewayConstants;
import org.mifos.mifospaymentbridge.PaymentProviders.Beyonic.PaymentRequest;
import org.mifos.mifospaymentbridge.PaymentProviders.Beyonic.PaymentResponse;
import org.mifos.mifospaymentbridge.PaymentProviders.Beyonic.PaymentService;
import org.mifos.mifospaymentbridge.Util.HostConfig;
import org.mifos.mifospaymentbridge.model.MobileMoneyProvider;
import org.mifos.mifospaymentbridge.model.OutboundRequest;
import org.mifos.mifospaymentbridge.model.Status;
import org.mifos.mifospaymentbridge.services.MobileMoneyProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import retrofit2.Response;
import java.io.IOException;

public abstract class BaseController {

    public static final String FINERACT_API_PATH = "/api/v1";

    public static final String LOAN_URL = FINERACT_API_PATH + "/loans/{loanId}";

    public static final String WITHDRAWAL_URL = FINERACT_API_PATH + "/savingsaccounts/{accountsId}/transactions";

    @Autowired
    private PaymentService beyonicService;

    @Autowired
    private MobileMoneyProviderService mobileMoneyProviderService;

    @Autowired
    private HostConfig hostConfig;

    public Response<PaymentResponse> makePayment(OutboundRequest request, Status status) {
        //get mmp for this payment
        MobileMoneyProvider mmp = mobileMoneyProviderService.findOne(request.getMmpId());

        System.out.println(mmp.toString());



        Response<PaymentResponse> resp = null;


        if(mmp != null){
            switch(mmp.getName()){
                case "Beyonic":
                    PaymentRequest beyonicReq = new PaymentRequest();
                    beyonicReq.setPhonenumber(request.getDestinationRef());
                    beyonicReq.setCurrency(mmp.getCurrencyCode());
                    beyonicReq.setAmount(request.getAmount());
                    beyonicReq.setAccount(null);
                    beyonicReq.setPayment_type("money");
                    beyonicReq.setDescription(request.getTransactionReason());
                    beyonicReq.setCallback_url(" https://68d7638a.ngrok.io/beyonic/payments/callback");
                    //beyonicReq.setCallback_url(String.format("%s://%s:%d/beyonic/payments/callback", hostConfig.getProtocol(),
                    //hostConfig.getHostName(), hostConfig.getPort()));

                    try {
                        resp = beyonicService.getPaymentResponse(beyonicReq, status);
                    } catch (IOException e) {

                        //Persist exception in status
                        status.setCode(String.valueOf(GatewayConstants.SYSTEM_ERROR_CODE));
                        status.setDescription(e.getMessage());
                        status.setStatusCategory(GatewayConstants.SYSTEM_STATUS);

                        e.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }

        return resp;

        /*switch(request.getPaymentMethod()){
            case "mobile money":
                switch(request.getPaymentMethodType()){
                    case "beyonic":
                        break;
                    default:
                        break;
                }
            default:
                break;
        }*/
    }
}
