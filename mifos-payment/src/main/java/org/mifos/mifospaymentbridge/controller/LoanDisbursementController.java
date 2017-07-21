/**
 * LoanDisbursementController.java
 * =======================================
 * @RestController that handles all http requests
 * for loan disbursement via the payment gateway
 * @author vladimir fomene
 */

package org.mifos.mifospaymentbridge.controller;

import org.mifos.mifospaymentbridge.Constant.GatewayConstants;
import org.mifos.mifospaymentbridge.PaymentProviders.Beyonic.PaymentRequest;
import org.mifos.mifospaymentbridge.PaymentProviders.Beyonic.PaymentResponse;
import org.mifos.mifospaymentbridge.PaymentProviders.Beyonic.PaymentService;
import org.mifos.mifospaymentbridge.domain.OutboundRequest;
import org.mifos.mifospaymentbridge.model.MobileMoneyProvider;
import org.mifos.mifospaymentbridge.model.OutboundTransactionLog;
import org.mifos.mifospaymentbridge.model.Status;
import org.mifos.mifospaymentbridge.services.MobileMoneyProviderService;
import org.mifos.mifospaymentbridge.services.OutboundRequestService;
import org.mifos.mifospaymentbridge.services.OutboundTransactionLogService;
import org.mifos.mifospaymentbridge.services.StatusService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class LoanDisbursementController extends BaseController{

    private Status disbursementStatus;

    @RequestMapping(value = LOAN_URL, method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OutboundRequest> disburseLoan(
            @RequestParam(value = "command", defaultValue = "disburseForPayment") String disburse,
            @PathVariable(value = "loanId") Long id,
            @RequestBody OutboundRequest request){


        //Send transaction to Beyonic
        PaymentResponse response = makePayment(request);

        //Update the external system id of the request with id of the payment response
        request.setExternalSystId(String.valueOf(response.getId()));

        //Save the outbound request in the gateway database.
        OutboundRequestService outboundRequestService = new OutboundRequestService();
        OutboundRequest req = outboundRequestService.save(request);


        if(response != null){
            //Create a status for our transaction and persist it to the database
            StatusService statusService = new StatusService();
            disbursementStatus = new Status();
            disbursementStatus.setCode(String.valueOf(response.getState()));
            disbursementStatus.setDescription("Created new beyonic transaction.");
            disbursementStatus.setStatusCategory(GatewayConstants.PAYMENT_STATUS);
            disbursementStatus = statusService.save(disbursementStatus);

            //Prepare to log transaction to the gateway database.
            OutboundTransactionLogService logService = new OutboundTransactionLogService();
            OutboundTransactionLog log = new OutboundTransactionLog();
            log.setOutboundRequestId(req.getId());
            log.setRequestIpAddress(req.getRequestIpAddress());
            log.setTransactType(req.getTransactType());
            log.setTransactionStatusId(req.getOutboundStatusId());
            log.setTransactionDtm(req.getOutboundStatusDtm());
            log.setTransactionStatusId(disbursementStatus.getId());
            logService.save(log);


        }




        return new ResponseEntity<OutboundRequest>(req, HttpStatus.CREATED);

    }

    private PaymentResponse makePayment(OutboundRequest request) {
        //get mmp for this payment
        MobileMoneyProviderService mmpService = new MobileMoneyProviderService();
        MobileMoneyProvider mmp = mmpService.findOne(request.getMmpId());

        PaymentResponse resp = null;


        if(mmp != null){
            switch(mmp.getName()){
                case "beyonic":
                    PaymentRequest beyonicReq = new PaymentRequest();
                    beyonicReq.setPhonenumber(request.getDestinationRef());
                    beyonicReq.setCurrency(mmp.getCurrencyCode());
                    beyonicReq.setPayment_type("money");
                    beyonicReq.setDescription(request.getTransactionReason());
                    beyonicReq.setCallback_url(request.getRequestIpAddress());
                    PaymentService beyonicService = new PaymentService();
                    try {
                        resp = beyonicService.getPaymentResponse(beyonicReq, disbursementStatus);
                    } catch (IOException e) {
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
