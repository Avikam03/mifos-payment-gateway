/**
 * File: PaymentController.java
 * ==========================================
 * This controller is in charge of handling payment notifications
 * from beyonic via its callback url
 * @author vladimir fomene
 */


package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;

import org.mifos.mifospaymentbridge.Constant.GatewayConstants;
import org.mifos.mifospaymentbridge.model.OutboundRequest;
import org.mifos.mifospaymentbridge.model.OutboundTransactionLog;
import org.mifos.mifospaymentbridge.model.Status;
import org.mifos.mifospaymentbridge.services.OutboundRequestService;
import org.mifos.mifospaymentbridge.services.OutboundTransactionLogService;
import org.mifos.mifospaymentbridge.services.StatusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private static final String PAYMENT_CALLBACK_URL = "/beyonic/payments/callback";

    private static final String COLLECTION_CALLBACK_URL = "/beyonic/collections/callback";

    @Autowired
    private OutboundRequestService outboundService;

    @Autowired
    private OutboundTransactionLogService logService;

    @Autowired
    private StatusService statusService;


    @RequestMapping(value = PAYMENT_CALLBACK_URL, method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    private ResponseEntity<String> paymentCallback(@RequestBody PaymentStateNotification notif){

        //Get outbound request corresponding to this notification
        OutboundRequest req = outboundService.findByExternalSystId(notif.getData().getId());

        //Get outboundTransactionLog object corresponding to this outbound request.
        OutboundTransactionLog transactionLog = logService.findByOutboundRequestId(req.getId());

        //Update status of transaction log
        Status status = statusService.findOne(transactionLog.getTransactionStatusId());
        status.setCode(notif.getData().getState());
        status.setDescription(notif.getData().getDescription());
        status.setStatusCategory(GatewayConstants.PAYMENT_STATUS);

        //Save new status
        statusService.save(status);

        return new ResponseEntity("Status Received", HttpStatus.CREATED);

    }

    @RequestMapping(value = COLLECTION_CALLBACK_URL, method = RequestMethod.POST,
            consumes = MediaType.APPLICATION_JSON_VALUE)
    private ResponseEntity<String> collectionCallback(@RequestBody CollectionNotification notif){

        return new ResponseEntity("Collection received", HttpStatus.CREATED);
    }
}
