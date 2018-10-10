package com.github.upatovav.rubUsdUiCalculator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import lombok.Data;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for exchange with fixer.io and calculating profit of currency investments if sold today
 */
public class ExchangeService {
    private static ExchangeService ourInstance = new ExchangeService();

    public static ExchangeService getInstance() {
        return ourInstance;
    }

    private static BigDecimal SPREAD_VALUE = new BigDecimal("0.005");

    private HashMap<LocalDate, BigDecimal> usdToRubRatesMap = new HashMap<>();

    private WebTarget webTarget;

    private ExchangeService() {
        Client client = ClientBuilder.newClient().register(JacksonJsonProvider.class);
        webTarget
                = client.target("http://data.fixer.io/api/")
                .queryParam("access_key","679a6b8ac99e73fa71800b2519fe9a5a")
                .queryParam("symbols", "USD,RUB");
    }

    /**
     * calculate profit of currency investment in case it is sold today
     * @param amount amount in USD
     * @param purchaseDate date of purchasing
     * @return
     */
    public BigDecimal calculateProfit(BigDecimal amount, LocalDate purchaseDate){

        BigDecimal currentRubAmount = amount.multiply(
                getExchangeRateForDate(LocalDate.now())
                        .multiply(BigDecimal.ONE.subtract(SPREAD_VALUE)));//substract spread value to rate of selling currency

        BigDecimal boughtRubAmount = amount.multiply(
                getExchangeRateForDate(purchaseDate)
                        .multiply(BigDecimal.ONE.add(SPREAD_VALUE))); //add spread value to rate of buying currency

        return currentRubAmount.subtract(boughtRubAmount);
    }

    private BigDecimal getExchangeRateForDate(LocalDate date){
        return usdToRubRatesMap.computeIfAbsent(date, this::getExchangeRate);
    }

    public BigDecimal getExchangeRate(LocalDate date){
        FixerResponce fixerResponce = webTarget.path(date.format(DateTimeFormatter.ISO_DATE))
                .request().get(FixerResponce.class);

        return fixerResponce.getRates().get("RUB")
                .divide(fixerResponce.getRates().get("USD"), RoundingMode.HALF_DOWN);

    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class FixerResponce{
        private Map<String, BigDecimal> rates;
    }
}
