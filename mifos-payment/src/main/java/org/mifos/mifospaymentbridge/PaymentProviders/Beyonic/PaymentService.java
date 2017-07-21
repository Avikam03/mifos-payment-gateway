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
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import java.io.IOException;


public class PaymentService {

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    private PaymentInterface paymentInterface;


    /**
     * Create payment interface class from PaymentInterface
     * interface using retrofit
     * @return PaymentInterface
     */
    private PaymentInterface createPaymentService(String endPoint){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(endPoint)
                .build();

        return retrofit.create(PaymentInterface.class);
    }

    /**
     * Takes in a payment request and sends out the request
     * to beyonic using our payment interface
     * @param request
     * @return response
     */
    private Response<PaymentResponse> sendPayment(PaymentRequest request){
        BeyonicProperties beyonicProp = new BeyonicProperties();
        paymentInterface = createPaymentService(beyonicProp.getEND_POINT());
        Call<PaymentResponse> paymentResponseCall = paymentInterface.createNewPayment(request.getPhonenumber(), request.getCurrency(),
                request.getAmount(), request.getDescription(),
                request.getCallback_url(), request.getPayment_type(),
                "Token " + beyonicProp.getAPI_TOKEN());

        Response<PaymentResponse> response = null;
        try{
            response = paymentResponseCall.execute();
        }catch(IOException ex){
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
    public PaymentResponse getPaymentResponse(PaymentRequest request, Status status) throws IOException {
        PaymentResponse response = null;
        Response<PaymentResponse> beyonicResponse = sendPayment(request);
        if(beyonicResponse.isSuccessful()) response = beyonicResponse.body();
        else{
            //Add error to the logs if not successful
            status.setCode(String.valueOf(beyonicResponse.code()));
            status.setDescription(beyonicResponse.errorBody().string());
            status.setStatusCategory(GatewayConstants.PAYMENT_API_STATUS);
        }
        return response;
    }

}
