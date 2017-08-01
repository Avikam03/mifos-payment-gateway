package org.mifos.mifospaymentbridge.PaymentProviders.Beyonic;


import retrofit2.Call;
import retrofit2.http.*;

public interface PaymentInterface {

    @Headers("Authorization: Token 71650dd01ae0fd4e3efdfc9764f9d12308b7244f")
    @POST("payments")
    Call<PaymentResponse> createNewPayment(@Body PaymentRequest request);
}
