/**
 * MifosService.java
 * =================================================
 * This class is a service to carry out savings and loan
 * operations for a particular client on mifos. This code was
 * adapted from @Antony Omeri's implementation
 * @author Vladimir Fomene
 */

package org.mifos.mifospaymentbridge.mifos;

import okhttp3.ResponseBody;
import org.mifos.mifospaymentbridge.Util.ApiClient;
import org.mifos.mifospaymentbridge.mifos.api.ClientInterface;
import org.mifos.mifospaymentbridge.mifos.api.LoanInterface;
import org.mifos.mifospaymentbridge.mifos.api.SavingsAccountInterface;
import org.mifos.mifospaymentbridge.mifos.domain.client.Client;
import org.mifos.mifospaymentbridge.mifos.domain.loan.Loan;
import org.mifos.mifospaymentbridge.mifos.domain.loan.disbursement.LoanDisbursementRequest;
import org.mifos.mifospaymentbridge.mifos.domain.loan.disbursement.LoanDisbursementResponse;
import org.mifos.mifospaymentbridge.mifos.domain.loan.repayment.LoanRepaymentRequest;
import org.mifos.mifospaymentbridge.mifos.domain.loan.repayment.LoanRepaymentResponse;
import org.mifos.mifospaymentbridge.mifos.domain.loan.undodisbursal.UndoLoanDisbursementRequest;
import org.mifos.mifospaymentbridge.mifos.domain.loan.undodisbursal.UndoLoanDisbursementResponse;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.deposit.AccountDepositRequest;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.deposit.AccountDepositResponse;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.deposit.RecurringDepositAccount;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.transaction.SavingsAccountTransaction;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.transaction.SavingsAccountTransactionUndoResponse;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.withdrawal.SavingsAccountWithdrawRequest;
import org.mifos.mifospaymentbridge.mifos.domain.savingsaccount.withdrawal.SavingsAccountWithdrawResponse;
import org.mifos.mifospaymentbridge.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.IOException;

@Service
public class MifosService {
    private ClientInterface clientInterface = null;
    private SavingsAccountInterface savingsAccountInterface = null;
    private LoanInterface loanInterface = null;

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public MifosService(MifosProperties mifosProperties) {
        String mifosApiUrl = mifosProperties.getBaseUrl() + mifosProperties.getApiVersion();
        logger.info("Creating mifos service with baseUrl: {}", mifosApiUrl);
        clientInterface = ApiClient.getClient(mifosApiUrl, mifosProperties.getUsername(), mifosProperties.getPassword()).create(ClientInterface.class);
    }



    public Client getClientByPhoneNumber(String tenantIdentifier, boolean isPretty, String phoneNumber) throws IOException{
        Client client = null;
        Call<Client> call = clientInterface.getClientByPhoneNumber(tenantIdentifier, isPretty, phoneNumber);
        Response<Client> clientResponse = call.execute();
        boolean isSuccessful = clientResponse.isSuccessful();
        int code = clientResponse.code();
        if (isSuccessful) {
           client = clientResponse.body();
           if (client != null) {
               logger.info("- getClientByPhoneNumber({},{},{}) :Response [isSuccessful: {}, code: {}, client: {}]", tenantIdentifier, isPretty, phoneNumber, isSuccessful, code, client);
           }
        } else {
           ResponseBody errorResponse = clientResponse.errorBody();
           if (errorResponse != null) {
               logger.info("- getClientByPhoneNumber({},{},{}) :Response [isSuccessful: {}, code: {}, error: {}]", tenantIdentifier, isPretty, phoneNumber, isSuccessful, code, errorResponse.string());
           }
        }
        return client;
    }


    public void getClientByID(Long clientId, String tenantIdentifier, boolean isPretty, Callback<Client> callback) throws IOException {
        Client client = null;
        Call<Client> call = clientInterface.getClientById(clientId, tenantIdentifier, isPretty);
        call.enqueue(callback);
    }


    public void depositToSavingsAccount(String accountsNo, AccountDepositRequest depositRequest,
                                                          boolean isPretty, String tenantIdentifier,
                                                          Callback<AccountDepositResponse> callback) throws IOException {
        AccountDepositResponse depositResponse = null;
        Call<AccountDepositResponse> call = savingsAccountInterface.deposit(accountsNo, depositRequest, isPretty, tenantIdentifier);
        call.enqueue(callback);
    }

    public void recurringSaving(String accountsNo, AccountDepositRequest depositRequest,
                                                  boolean isPretty, String tenantIdentifier, Callback<AccountDepositResponse> callback) throws IOException {
        AccountDepositResponse depositResponse = null;
        Call<AccountDepositResponse> call = savingsAccountInterface.recurringSaving(accountsNo, depositRequest, isPretty, tenantIdentifier);
        call.enqueue(callback);
    }


    public SavingsAccountWithdrawResponse withdrawFromSavingsAccount(Long accountsId, SavingsAccountWithdrawRequest withdrawRequest, boolean isPretty, String tenantIdentifier) throws IOException {
        SavingsAccountWithdrawResponse withdrawResponse = null;
        Call<SavingsAccountWithdrawResponse> call = savingsAccountInterface.withdraw(accountsId, withdrawRequest, isPretty, tenantIdentifier);
        Response<SavingsAccountWithdrawResponse> response = call.execute();
        boolean isSuccessful = response.isSuccessful();
        int code = response.code();
        if (isSuccessful) {
            withdrawResponse = response.body();
            if (withdrawResponse != null) {
                logger.info("- withdrawFromSavingsAccount({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, withdrawResponse: {}]", accountsId, withdrawRequest, isPretty, tenantIdentifier, isSuccessful, code, withdrawResponse);

            } else {
                ResponseBody errorResponse = response.errorBody();
                if (errorResponse != null) {
                    logger.info("- withdrawFromSavingsAccount({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", accountsId, withdrawRequest, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
                }
            }
        } else {
            ResponseBody errorResponse = response.errorBody();
            if (errorResponse != null) {

            }
        }
        return withdrawResponse;
    }


    public SavingsAccountTransaction getSavingsAccountTransaction(Long accountsId, Long transactionId, boolean isPretty, String tenantIdentifier) throws IOException {
        SavingsAccountTransaction accountTransaction = null;
        Call<SavingsAccountTransaction> call = savingsAccountInterface.getTransaction(accountsId, transactionId, isPretty, tenantIdentifier);
        Response<SavingsAccountTransaction> response = call.execute();
        boolean isSuccessful = response.isSuccessful();
        int code = response.code();
        if (isSuccessful) {
            accountTransaction = response.body();
            if (accountTransaction != null) {
                logger.info("- getSavingsAccountTransaction({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, accountTransaction: {}]", accountsId, transactionId, isPretty, tenantIdentifier, isSuccessful, code, accountTransaction);
            } else {
                ResponseBody errorResponse = response.errorBody();
                if (errorResponse != null) {
                    logger.info("- getSavingsAccountTransaction({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", accountsId, transactionId, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
                }
            }
        }else {
            ResponseBody errorResponse = response.errorBody();
            if (errorResponse != null) {
                logger.info("- getSavingsAccountTransaction({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", accountsId, transactionId, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
            }
        }
        return accountTransaction;
    }


    public SavingsAccountTransactionUndoResponse undoSavingsAccountTransaction(Long accountsId, Long transactionId, boolean isPretty, String tenantIdentifier) throws IOException {
        SavingsAccountTransactionUndoResponse reversedTransaction = null;
        Call<SavingsAccountTransactionUndoResponse> call = savingsAccountInterface.undoSavingsAccountTransaction(accountsId, transactionId, isPretty, tenantIdentifier);
        Response<SavingsAccountTransactionUndoResponse> response = call.execute();
        boolean isSuccessful = response.isSuccessful();
        int code = response.code();
        if (isSuccessful) {
            reversedTransaction = response.body();
            if (reversedTransaction != null) {
                logger.info("- undoSavingsAccountTransaction({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, accountTransaction: {}]", accountsId, transactionId, isPretty, tenantIdentifier, isSuccessful, code, reversedTransaction);
            } else {
                ResponseBody errorResponse = response.errorBody();
                if (errorResponse != null) {
                    logger.info("- undoSavingsAccountTransaction({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", accountsId, transactionId, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
                }
            }
        }else {
            ResponseBody errorResponse = response.errorBody();
            if (errorResponse != null) {
                logger.info("- undoSavingsAccountTransaction({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", accountsId, transactionId, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
            }
        }
        return reversedTransaction;
    }


    public LoanDisbursementResponse disburse(Long loanId,
                                             LoanDisbursementRequest loanDisbursementRequest,
                                             boolean isPretty,
                                             String tenantIdentifier) throws IOException{
        LoanDisbursementResponse disburseResponse = null;
        Call<LoanDisbursementResponse> call = loanInterface.disburse(loanId, loanDisbursementRequest, isPretty, tenantIdentifier);
        Response<LoanDisbursementResponse> response = call.execute();

        boolean isSuccessful = response.isSuccessful();
        int code = response.code();
        if (isSuccessful) {
            disburseResponse = response.body();
            if (disburseResponse != null) {
                logger.info("- disburse({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, accountTransaction: {}]", loanId, loanDisbursementRequest, isPretty, tenantIdentifier, isSuccessful, code, disburseResponse);
            } else {
                ResponseBody errorResponse = response.errorBody();
                if (errorResponse != null) {
                    logger.info("- disburse({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", loanId, loanDisbursementRequest, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
                }
            }
        }else {
            ResponseBody errorResponse = response.errorBody();
            if (errorResponse != null) {
                logger.info("- disburse({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", loanId, loanDisbursementRequest, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
            }
        }

        return disburseResponse;
    }


    public void repay(Long loanId,
                      LoanRepaymentRequest loanRepaymentRequest,
                      boolean isPretty,
                      String tenantIdentifier,
                      Callback<LoanRepaymentResponse> callback) throws IOException{
        LoanRepaymentResponse repaymentResponse = null;
        Call<LoanRepaymentResponse> call = loanInterface.repay(loanId, loanRepaymentRequest, isPretty, tenantIdentifier);
        call.enqueue(callback);

    }


    public UndoLoanDisbursementResponse undoDisbursal(Long loanId,
                                                      UndoLoanDisbursementRequest undoLoanDisbursementRequest,
                                                      boolean isPretty,
                                                      String tenantIdentifier) throws IOException{
        UndoLoanDisbursementResponse undoDisbursalResponse = null;
        Call<UndoLoanDisbursementResponse> call = loanInterface.undoDisbursal(loanId, undoLoanDisbursementRequest, isPretty, tenantIdentifier);
        Response<UndoLoanDisbursementResponse> response = call.execute();

        boolean isSuccessful = response.isSuccessful();
        int code = response.code();
        if (isSuccessful) {
            undoDisbursalResponse = response.body();
            if (undoDisbursalResponse != null) {
                logger.info("- undoDisbursal({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, accountTransaction: {}]", loanId, undoLoanDisbursementRequest, isPretty, tenantIdentifier, isSuccessful, code, undoDisbursalResponse);
            } else {
                ResponseBody errorResponse = response.errorBody();
                if (errorResponse != null) {
                    logger.info("- undoDisbursal({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", loanId, undoLoanDisbursementRequest, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
                }
            }
        }else {
            ResponseBody errorResponse = response.errorBody();
            if (errorResponse != null) {
                logger.info("- undoDisbursal({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", loanId, undoLoanDisbursementRequest, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
            }
        }

        return undoDisbursalResponse;
    }

    public void undoDisbursalAsync(Long loanId, UndoLoanDisbursementRequest undoLoanDisbursementRequest, boolean
            isPretty, String tenantIdentifier, Callback<UndoLoanDisbursementResponse> callback) throws IOException{
        Call<UndoLoanDisbursementResponse> call = loanInterface.undoDisbursal(loanId, undoLoanDisbursementRequest, true, "default");
        call.enqueue(callback);
    }

    public void getLoanAccountAsync(String loanAccNo, boolean isPretty, String tenantIdentifier, Callback<Loan> callback) throws IOException{
        Call<Loan> call = loanInterface.getLoanAccount(loanAccNo, isPretty, tenantIdentifier);
        call.enqueue(callback);

    }


    public Loan getLoanAccount(String loanAccNo, boolean isPretty, String tenantIdentifier) throws IOException{
        Loan loanAccount = null;
        Call<Loan> call = loanInterface.getLoanAccount(loanAccNo, isPretty, tenantIdentifier);

        Response<Loan> response = call.execute();

        boolean isSuccessful = response.isSuccessful();
        int code = response.code();
        if (isSuccessful) {
            loanAccount = response.body();
            if (loanAccount != null) {
                logger.info("- getLoanAccount({}, {}, {}) :Response [isSuccessful: {}, code: {}, accountTransaction: {}]", loanAccNo, isPretty, tenantIdentifier, isSuccessful, code, loanAccount);
            } else {
                ResponseBody errorResponse = response.errorBody();
                if (errorResponse != null) {
                    logger.info("- getLoanAccount({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", loanAccNo, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
                }
            }
        }else {
            ResponseBody errorResponse = response.errorBody();
            if (errorResponse != null) {
                logger.info("- getLoanAccount({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", loanAccNo, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
            }
        }

        return loanAccount;

    }


    public void getRecurringDepositAccountAsync(String depositAccNo, boolean isPretty,
                                                              String tenantIdentifier,
                                                              Callback<RecurringDepositAccount> callback) throws IOException{

        Call<RecurringDepositAccount> call = savingsAccountInterface.getRecurringDepositAccount(depositAccNo, isPretty, tenantIdentifier);
        call.enqueue(callback);
    }


    public RecurringDepositAccount getRecurringDepositAccount(String depositAccNo, boolean isPretty,
                                                String tenantIdentifier) throws IOException{

        RecurringDepositAccount depositAccount = null;
        Call<RecurringDepositAccount> call = savingsAccountInterface.getRecurringDepositAccount(depositAccNo, isPretty, tenantIdentifier);

        Response<RecurringDepositAccount> response = call.execute();

        boolean isSuccessful = response.isSuccessful();
        int code = response.code();
        if (isSuccessful) {
            depositAccount = response.body();
            if (depositAccount != null) {
                logger.info("- getRecurringDepositAccount({}, {}, {}) :Response [isSuccessful: {}, code: {}, accountTransaction: {}]", depositAccNo, isPretty, tenantIdentifier, isSuccessful, code, depositAccount);
            } else {
                ResponseBody errorResponse = response.errorBody();
                if (errorResponse != null) {
                    logger.info("- getRecurringDepositAccount({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", depositAccNo, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
                }
            }
        }else {
            ResponseBody errorResponse = response.errorBody();
            if (errorResponse != null) {
                logger.info("- getRecurringDepositAccount({}, {}, {}, {}) :Response [isSuccessful: {}, code: {}, error: {}]", depositAccNo, isPretty, tenantIdentifier, isSuccessful, code, errorResponse.string());
            }
        }

        return depositAccount;

    }
}
