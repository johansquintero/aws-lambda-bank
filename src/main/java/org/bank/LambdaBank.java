package org.bank;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.bank.dto.BankRequest;
import org.bank.dto.BankResponse;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * P = MONTO DEL PESTAMO
 * i = TASA DE INTERES MENSUAL
 * n = PLAZO DEL CREDITO A MESES
 * <p>
 * cuota mensual = (P*i)/(1-(1+i)^(-n))
 */
public class LambdaBank implements RequestHandler<BankRequest, BankResponse> {
    @Override
    public BankResponse handleRequest(BankRequest bankRequest, Context context) {
        MathContext mathContext = MathContext.DECIMAL128;

        BigDecimal amount = bankRequest.getAmount()
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal rate = bankRequest.getRate()
                .setScale(2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100),mathContext);

        BigDecimal rateWithAccount = bankRequest
                .getRate()
                .subtract(BigDecimal.valueOf(0.2), mathContext)
                .setScale(2, RoundingMode.HALF_UP)
                .divide(BigDecimal.valueOf(100), mathContext);

        Integer term = bankRequest.getTerm();
        BigDecimal monthlyPayment = this.calculateQuota(amount,rate,term,mathContext);
        BigDecimal monthlyPaymentWithAccount = this.calculateQuota(amount,rateWithAccount,term,mathContext);
        BankResponse bankResponse = new BankResponse();
        bankResponse.setQuota(monthlyPayment);
        bankResponse.setRate(rate.multiply(BigDecimal.valueOf(100),mathContext));
        bankResponse.setTerm(term);
        bankResponse.setQuotaWithAccount(monthlyPaymentWithAccount);
        bankResponse.setRateWithAccount(rateWithAccount.multiply(BigDecimal.valueOf(100),mathContext));
        bankResponse.setTermWithAccount(term);

        return bankResponse;
    }

    public BigDecimal calculateQuota(BigDecimal amount, BigDecimal rate, Integer term, MathContext mathContext) {
        //calcula 1+i
        BigDecimal a = rate.add(BigDecimal.valueOf(1), mathContext);

        //calcular (1+i)^(n)
        BigDecimal b = a.pow(term, mathContext); //reciproco de un numero

        //reciproco (1+i)^(-n) = 1/((1+i)^(n))
        BigDecimal c = BigDecimal.ONE.divide(b, mathContext);

        //calcular 1 + (1+i)^(-n)
        BigDecimal denominator = c.add(BigDecimal.ONE, mathContext);

        //calcular (P*i)
        BigDecimal numerator = amount.multiply(rate, mathContext);

        //calcular (P*i)/(1-(1+i)^(-n))
        BigDecimal result = numerator.divide(denominator, mathContext);
        result = result.setScale(2, RoundingMode.HALF_UP);
        return result;
    }
}