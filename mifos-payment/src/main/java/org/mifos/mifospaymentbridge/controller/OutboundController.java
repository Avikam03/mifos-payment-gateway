package org.mifos.mifospaymentbridge.controller;


import org.mifos.mifospaymentbridge.integration.OutboundMessageReceiver;
import org.mifos.mifospaymentbridge.model.OutboundRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class OutboundController {

    @Autowired
    private OutboundMessageReceiver receiver;

    @RequestMapping(value = "/outbound/requests", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> acceptOutboundRequest(@RequestBody OutboundRequest outboundRequest, @RequestParam(value="tenant") String tenant){
        System.out.println(outboundRequest.toString());
        receiver.receiveRequest(outboundRequest);

        return new ResponseEntity<>("Outbound request received", HttpStatus.OK);
    }

}
