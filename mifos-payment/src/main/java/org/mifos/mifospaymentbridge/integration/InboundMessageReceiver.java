package org.mifos.mifospaymentbridge.integration;

import org.mifos.mifospaymentbridge.Util.StatusCategory;
import org.mifos.mifospaymentbridge.Util.TransactionStatus;
import org.mifos.mifospaymentbridge.integration.ProviderApiService.PaymentService;
import org.mifos.mifospaymentbridge.mifos.MifosService;
import org.mifos.mifospaymentbridge.mifos.domain.loan.Loan;
import org.mifos.mifospaymentbridge.mifos.domain.loan.repayment.LoanRepaymentRequest;
import org.mifos.mifospaymentbridge.mifos.domain.loan.repayment.LoanRepaymentResponse;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.deposit.AccountDepositRequest;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.deposit.AccountDepositResponse;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.deposit.RecurringDepositAccount;
import org.mifos.mifospaymentbridge.model.InboundCallbackLog;
import org.mifos.mifospaymentbridge.model.InboundRequest;
import org.mifos.mifospaymentbridge.model.MobileMoneyProvider;
import org.mifos.mifospaymentbridge.model.Status;
import org.mifos.mifospaymentbridge.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import javax.persistence.criteria.CriteriaBuilder;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Created by vladimirfomene on 8/4/17.
 */
public class InboundMessageReceiver {

    private Loan loanAccount;

    private RecurringDepositAccount depositAccount;

    private InboundCallbackLog callbackLog;

    private LoanRepaymentResponse repaymentResponse;

    private Status callbackStatus = new Status();

    private Status inboundStatus = new Status();

    private LoanRepaymentRequest repaymentRequest;

    private AccountDepositResponse depositResponse;

    private InboundRequest inboundRequest;


    @Autowired
    private InboundRequestService inboundRequestService;

    @Autowired
    private InboundCallbackLogService inboundCallbackLogService;

    @Autowired
    private StatusService statusService;

    @Autowired
    private MobileMoneyProviderService mmpService;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private MifosService mifosService;

    @Autowired
    private PaymentService paymentService;


    public InboundMessageReceiver(){

    }

    @JmsListener(destination = "inboundAcceptor", containerFactory = "inboundFactory")
    public void receiveRequest(InboundRequest msg){
        handleTransaction(msg);
    }

    public void handleTransaction(InboundRequest request){

        //Query that particular request
        inboundRequest = inboundRequestService.findInboundRequestByFineractAccNo(request.getFineractAccNo());

        //Create inbound request object
        buildRequest(request);
        //handle request per payment method
        if(inboundRequest.getPaymentMethod().equalsIgnoreCase("mobile money")){

            //mmp lookup
            MobileMoneyProvider mmp = mmpService.findOne(inboundRequest.getMmpId());

            //Log the request to the database
            inboundRequest = inboundRequestService.save(inboundRequest);

            //send request to fineract
            if (inboundRequest.getTransactType() == InboundRequest.TransactionType.LOAN_REPAYMENT) {
                initiateLoanRepayment(inboundRequest);
            }


            if (inboundRequest.getTransactType() == InboundRequest.TransactionType.VOLUNTARY_SAVINGS) {
                initiateVoluntarySaving(inboundRequest);
            }

            if(inboundRequest.getTransactType() == InboundRequest.TransactionType.FIXED_DEPOSIT){
                initiateReccuringDeposit(inboundRequest);
            }

            String callbackUrl = configurationService.findConfigurationByConfigNameAndReferenceId("callback_url",
                    inboundRequest.getMmpId()).getConfigValue();

            //Create callback log and save it.
            callbackLog = new InboundCallbackLog();
            callbackLog.setInboundRequestId(inboundRequest.getId());
            callbackLog.setCallbackUrl(callbackUrl);
            callbackLog.setCallbackMessage(callbackStatus.getDescription());
            callbackLog.setCallbackDtm(Timestamp.valueOf(LocalDateTime.now()));
            inboundCallbackLogService.save(callbackLog);

            //Log callback status and send it
            statusService.save(callbackStatus);
            callbackStatus = paymentService.sendInboundTransactionStatus(callbackUrl, callbackStatus);

        }else if(inboundRequest.getPaymentMethod().equalsIgnoreCase("bit coin")){

        }
        
    }

    private void buildRequest(InboundRequest request){

        inboundRequest.setId(request.getId());
        inboundRequest.setTransactType(request.getTransactType());
        inboundRequest.setMmpId(request.getMmpId());
        inboundRequest.setMfiId(request.getMfiId());
        inboundRequest.setSourceRef(request.getSourceRef());
        inboundRequest.setDestinationRef(request.getDestinationRef());
        inboundRequest.setFineractAccNo(request.getFineractAccNo());
        inboundRequest.setFineractClientId(request.getFineractClientId());
        inboundRequest.setAmount(request.getAmount());
        inboundRequest.setTransactionReason(request.getTransactionReason());
        inboundRequest.setExternalSystId(request.getExternalSystId());
        inboundRequest.setComments(request.getComments());
        inboundRequest.setRequestedDtm(request.getRequestedDtm());
        inboundRequest.setRequestIpAddress(request.getRequestIpAddress());
        inboundRequest.setInboundStatusId(request.getInboundStatusId());
        inboundRequest.setInboundStatusDtm(request.getInboundStatusDtm());
        inboundRequest.setPaymentMethod(request.getPaymentMethod());
        inboundRequest.setPaymentMethodType(request.getPaymentMethodType());
    }

    private void initiateLoanRepayment(InboundRequest request){
        //build a loan repayment object
        repaymentRequest = new LoanRepaymentRequest();
        repaymentRequest.setAccountNumber(request.getFineractAccNo());
        repaymentRequest.setTransactionAmount(String.valueOf(request.getAmount()));
        repaymentRequest.setDateFormat("dd MMMM yyyy");
        repaymentRequest.setNote(request.getTransactionReason());
        repaymentRequest.setTransactionDate(request.getRequestedDtm().toString());

        //Get the loan account and repay it
        getLoanAccountAndRepayIt(request);

    }

    private void initiateVoluntarySaving(InboundRequest request){
        AccountDepositRequest depositRequest = new AccountDepositRequest();
        depositRequest.setTransactionAmount(String.valueOf(request.getAmount()));
        depositRequest.setAccountNumber(request.getFineractAccNo());
        depositRequest.setTransactionDate(request.getInboundStatusDtm().toString());

        sendVoluntarySavingRequest(depositRequest, request);
    }

    private void initiateReccuringDeposit(InboundRequest request){

        //Build an account deposit request object
        AccountDepositRequest depositRequest = new AccountDepositRequest();
        depositRequest.setTransactionAmount(String.valueOf(request.getAmount()));
        depositRequest.setAccountNumber(request.getFineractAccNo());
        depositRequest.setTransactionDate(request.getInboundStatusDtm().toString());

        //Get the recurring deposit account from fineract and make a deposit
        getRecurringDepositAccountAndMakeDeposit(request, depositRequest);


    }

    private void getLoanAccountAndRepayIt(InboundRequest request){
        try {
            mifosService.getLoanAccountAsync(request.getFineractAccNo(), true, "default", new Callback<Loan>() {
                @Override
                public void onResponse(Call<Loan> call, Response<Loan> response) {
                    if(response.isSuccessful()){
                        loanAccount = response.body();
                        callFineractForRepayment();
                    }else{
                        System.out.println(response.errorBody());
                    }
                }

                @Override
                public void onFailure(Call<Loan> call, Throwable t) {
                    System.out.println(t.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void callFineractForRepayment(){

        try {
            mifosService.repay(loanAccount.getId(), repaymentRequest, true, "default", new Callback<LoanRepaymentResponse>() {
                @Override
                public void onResponse(Call<LoanRepaymentResponse> call, Response<LoanRepaymentResponse> response) {
                    if(response.isSuccessful()){
                        repaymentResponse = response.body();

                        if(repaymentResponse != null){
                            inboundStatus.setCode(String.valueOf(TransactionStatus.REPAYMENT_SUCCESS_CODE));
                            inboundStatus.setDescription(TransactionStatus.REPAYMENT_SUCCESS);
                            inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                        }

                    }else{
                        System.out.println(response.errorBody());

                        inboundStatus.setCode(String.valueOf(TransactionStatus.REPAYMENT_FAILURE_CODE));
                        inboundStatus.setDescription(TransactionStatus.REPAYMENT_FAILURE);
                        inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                    }
                }

                @Override
                public void onFailure(Call<LoanRepaymentResponse> call, Throwable t) {

                    inboundStatus.setCode(String.valueOf(TransactionStatus.REPAYMENT_FAILURE_CODE));
                    inboundStatus.setDescription(TransactionStatus.REPAYMENT_FAILURE);
                    inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                    System.out.println(t.getMessage());
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void sendVoluntarySavingRequest(AccountDepositRequest depositRequest, InboundRequest request){
        try {
            mifosService.depositToSavingsAccount(request.getFineractAccNo(), depositRequest,
                    true, "default", new Callback<AccountDepositResponse>() {
                        @Override
                        public void onResponse(Call<AccountDepositResponse> call, Response<AccountDepositResponse> response) {
                            if(response.isSuccessful()){
                                depositResponse = response.body();
                                if(depositResponse != null){
                                    //Send response back to mmp
                                    inboundStatus.setCode(String.valueOf(TransactionStatus.VOLUNTARY_DEPOSIT_SUCCESS_CODE));
                                    inboundStatus.setDescription(TransactionStatus.VOLUNTARY_DEPOSIT_SUCCESS);
                                    inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                                }
                            }else{
                                System.out.println(response.errorBody());
                                inboundStatus.setCode(String.valueOf(TransactionStatus.VOLUNTARY_DEPOSIT_FAILURE_CODE));
                                inboundStatus.setDescription(TransactionStatus.VOLUNTARY_DEPOSIT_FAILURE);
                                inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                            }
                        }

                        @Override
                        public void onFailure(Call<AccountDepositResponse> call, Throwable t) {
                            System.out.println(t.getMessage());
                            inboundStatus.setCode(String.valueOf(TransactionStatus.VOLUNTARY_DEPOSIT_FAILURE_CODE));
                            inboundStatus.setDescription(TransactionStatus.VOLUNTARY_DEPOSIT_FAILURE);
                            inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private void getRecurringDepositAccountAndMakeDeposit(InboundRequest request, AccountDepositRequest depositRequest){
        try {
            mifosService.getRecurringDepositAccountAsync(request.getFineractAccNo(),
                    true, "default", new Callback<RecurringDepositAccount>() {
                        @Override
                        public void onResponse(Call<RecurringDepositAccount> call, Response<RecurringDepositAccount> response) {
                            if(response.isSuccessful()){
                                depositAccount = response.body();

                                callFineractForRecurringDeposit(request, depositRequest);
                            }else{
                                System.out.println(response.errorBody());
                            }

                        }

                        @Override
                        public void onFailure(Call<RecurringDepositAccount> call, Throwable t) {
                            System.out.println(t.getMessage());
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void callFineractForRecurringDeposit(InboundRequest request, AccountDepositRequest depositRequest){
        try {
            mifosService.recurringSaving(request.getFineractAccNo(), depositRequest,
                    true, "default", new Callback<AccountDepositResponse>() {
                        @Override
                        public void onResponse(Call<AccountDepositResponse> call, Response<AccountDepositResponse> response) {
                            if(response.isSuccessful()){
                                depositResponse = response.body();

                                //Save status for the transaction
                                inboundStatus.setCode(String.valueOf(TransactionStatus.RECURRING_DEPOSIT_SUCCESS_CODE));
                                inboundStatus.setDescription(TransactionStatus.RECURRING_DEPOSIT_SUCCESS);
                                inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);

                            }else{

                                inboundStatus.setCode(String.valueOf(TransactionStatus.RECURRING_DEPOSIT_FAILURE_CODE));
                                inboundStatus.setDescription(TransactionStatus.RECURRING_DEPOSIT_FAILURE);
                                inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                                System.out.println(response.errorBody());
                            }

                        }

                        @Override
                        public void onFailure(Call<AccountDepositResponse> call, Throwable t) {

                            inboundStatus.setCode(String.valueOf(TransactionStatus.RECURRING_DEPOSIT_FAILURE_CODE));
                            inboundStatus.setDescription(TransactionStatus.RECURRING_DEPOSIT_FAILURE);
                            inboundStatus.setStatusCategory(StatusCategory.FINERACT_CATEGORY);
                            System.out.println(t.getMessage());
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
