/**
 * File: PaymentController.java
 * ==========================================
 * This controller is in charge of handling payment notifications
 * from beyonic via its callback url
 * @author vladimir fomene
 */


package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;

import org.mifos.mifospaymentbridge.Constant.GatewayConstants;
import org.mifos.mifospaymentbridge.domain.OutboundRequest;
import org.mifos.mifospaymentbridge.model.OutboundTransactionLog;
import org.mifos.mifospaymentbridge.model.Status;
import org.mifos.mifospaymentbridge.services.OutboundRequestService;
import org.mifos.mifospaymentbridge.services.OutboundTransactionLogService;
import org.mifos.mifospaymentbridge.services.StatusService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private static final String PAYMENT_CALLBACK_URL = "/beyonic/payments/callback";

    @RequestMapping(value = PAYMENT_CALLBACK_URL, method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    private void paymentCallback(@RequestBody PaymentStateNotification notif){

        //Get outbound request corresponding to this notification
        OutboundRequestService outboundService = new OutboundRequestService();
        OutboundRequest req = outboundService.findOne(notif.getData().getId());

        //Get outboundTransactionLog object corresponding to this outbound request.
        OutboundTransactionLogService logService = new OutboundTransactionLogService();
        OutboundTransactionLog transactionLog = logService.findByOutboundRequestId(req.getId());

        //Update status of transaction log
        StatusService statusService = new StatusService();
        Status status = statusService.findOne(transactionLog.getTransactionStatusId());
        status.setCode(notif.getData().getState().getCode());
        status.setDescription(notif.getData().getDescription());
        status.setStatusCategory(GatewayConstants.PAYMENT_STATUS);

        //Save new status
        statusService.save(status);

    }
}
