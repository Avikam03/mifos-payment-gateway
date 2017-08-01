/**
 * File: PaymentService.java
 * ==================================
 * A collection of methods to interact with Beyonic's
 * payment gateway.
 */


package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;



import org.mifos.mifospaymentbridge.Constant.GatewayConstants;
import org.mifos.mifospaymentbridge.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import java.io.IOException;

@Service
public class PaymentService {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private PaymentInterface paymentInterface;

    @Autowired
    private BeyonicProperties beyonicProp;
    /**
     * Create payment interface class from PaymentInterface
     * interface using retrofit
     * @return PaymentInterface
     */
    private PaymentInterface createPaymentService(String endPoint){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endPoint)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();

        return retrofit.create(PaymentInterface.class);
    }

    /**
     * Takes in a payment request and sends out the request
     * to beyonic using our payment interface
     * @param request
     * @return response
     */
    private Response<PaymentResponse> sendPayment(PaymentRequest request, Status status){
        //System.out.println(beyonicProp.getEND_POINT());
        paymentInterface = createPaymentService("https://app.beyonic.com/api/");
        System.out.println(paymentInterface.toString());
        Call<PaymentResponse> paymentResponseCall = paymentInterface.createNewPayment(request);

        System.out.println(paymentResponseCall.toString());

        Response<PaymentResponse> response = null;
        try{
            response = paymentResponseCall.execute();
            System.out.println(response.toString());
        }catch(IOException ex){
            //In case of IO failure, record a status failure
            status.setCode(String.valueOf(GatewayConstants.SYSTEM_ERROR_CODE));
            status.setDescription(ex.getMessage());
            status.setStatusCategory(GatewayConstants.PAYMENT_API_STATUS);
            logger.error(ex.getMessage(), ex);
        }

        return response;
    }

    /**
     * Gets a payment response object from a payment
     * transaction and updates the status of the payment transaction
     * @param request
     * @param status
     * @return response
     * @throws IOException
     */
    public Response<PaymentResponse> getPaymentResponse(PaymentRequest request, Status status) throws IOException {
        PaymentResponse response = null;
        Response<PaymentResponse> beyonicResponse = sendPayment(request, status);
        System.out.println(beyonicResponse.body());
        if(beyonicResponse.isSuccessful()) return beyonicResponse;  //response = beyonicResponse.body();
        else{
            //Add error to the logs if not successful
            System.out.println(beyonicResponse.toString());
            System.out.println(beyonicResponse.errorBody().string());
            status.setCode(String.valueOf(beyonicResponse.code()));
            status.setDescription(beyonicResponse.errorBody().string());
            status.setStatusCategory(GatewayConstants.PAYMENT_API_STATUS);
        }
        return beyonicResponse;
    }

}
