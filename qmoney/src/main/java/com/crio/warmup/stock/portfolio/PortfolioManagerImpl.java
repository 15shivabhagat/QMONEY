
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


  public static final String TOKEN = "3c3b8db20bd3e85b8051a343cc639c78520ea71b";
  private RestTemplate restTemplate;

  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF
  // public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
  //     PortfolioTrade trade, Double buyPrice, Double sellPrice) {
    
  //   double totalReturn = (sellPrice - buyPrice) / buyPrice;
      
  //   double numYears = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365.24;
  //   double annualizedReturns = Math.pow((1 + totalReturn), (1 / numYears)) - 1;

  //   return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturn);
  // }



  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {

    if(from.compareTo(to) >=0) {
      throw new RuntimeException();
    }
    
    TiingoCandle[] stockStartToEndDate = restTemplate.getForObject(buildUri(symbol, from, to), TiingoCandle[].class);

    if (stockStartToEndDate == null) {
      return new ArrayList<Candle>();
    } else {
      List<Candle> stockList = Arrays.asList(stockStartToEndDate);
      return stockList;
    }
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
       String uriTemplate = "https://api.tiingo.com/tiingo/daily/" + symbol + "/prices?startDate=" + startDate.toString() + 
       "&endDate=" + endDate.toString() + "&token=" + TOKEN;
      //  "https:api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
      //       + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
      return uriTemplate;
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {
    // TODO Auto-generated method stub
    AnnualizedReturn annualizedReturn;
    List<AnnualizedReturn> annualizedReturns = new ArrayList<AnnualizedReturn>();
    for(int i = 0; i < portfolioTrades.size(); i++) {
      // Get Annualized Return object for each
      annualizedReturn = getAnnualizedReturn(portfolioTrades.get(i), endDate);

      // Add those to a list
      annualizedReturns.add(annualizedReturn);
    }
    System.out.println("Sorting Return");
    Comparator<AnnualizedReturn> SortByReturn = Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();

    Collections.sort(annualizedReturns, SortByReturn);
    return annualizedReturns;
  }


  private AnnualizedReturn getAnnualizedReturn(PortfolioTrade portfolioTrade, LocalDate endDate) {
    AnnualizedReturn annualizedReturn;
    String symbol = portfolioTrade.getSymbol();
    LocalDate startDate = portfolioTrade.getPurchaseDate();

    try {
      List<Candle> stocksStartToEndDate;
      stocksStartToEndDate = getStockQuote(symbol, startDate, endDate);

      // Extract stocks from start date to end 
      Candle stocksStartDate = stocksStartToEndDate.get(0);
      Candle stocksEndtDate = stocksStartToEndDate.get(stocksStartToEndDate.size() - 1);

      Double buyPrice = stocksStartDate.getOpen();
      Double sellPrice = stocksEndtDate.getClose();

      // Calculate Total return
      double totalReturn = (sellPrice - buyPrice) / buyPrice;

      // Calculate Year
      double numYears = ChronoUnit.DAYS.between(startDate, endDate) / 365.24;

      // Calculate Annualize Return 
      double annualizedReturns = Math.pow((1 + totalReturn), (1 / numYears)) - 1;

      annualizedReturn = new AnnualizedReturn(symbol, annualizedReturns, totalReturn);

    } catch(JsonProcessingException e) {
      annualizedReturn = new AnnualizedReturn(symbol, Double.NaN, Double.NaN);
    }

    return annualizedReturn;
  } 
}
