import ru.tinkoff.invest.openapi.model.rest.*;
import ru.tinkoff.invest.openapi.model.streaming.CandleInterval;
import ru.tinkoff.invest.openapi.model.streaming.StreamingEvent;
import ru.tinkoff.invest.openapi.model.streaming.StreamingRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Robor {
    private final MarketInstrument instrument;
    private OrderResponse[] bidsList = new OrderResponse[50];
    private OrderResponse[] asksList = new OrderResponse[50];
    private BigDecimal bidPrice;
    private BigDecimal askPrice;
    private BigDecimal lastPrice;
    private int lastQuantity;
    private TradingParameters parameters;
    private BigDecimal wallet;
    private boolean firstSell;
    private BigDecimal firstSellWallet = new BigDecimal(0);

    public Robor(MarketInstrument instrument, TradingParameters parameters, BigDecimal wallet) {
        this.parameters = parameters;
        this.instrument = instrument;
        this.wallet = wallet;
        this.firstSell = true;
        getOrderList();
    }

    public void start() {
        bidPrice = getMaxBidsPrice();
        askPrice = getMaxAsksPrice();
        TradingParameters.getLogger().info("Подписываемся на свечи " + instrument.getName());
        TradingParameters.getApi()
                        .getStreamingContext()
                        .sendRequest(StreamingRequest.subscribeCandle(instrument.getFigi(), CandleInterval._1MIN));
    }

    public BigDecimal getWallet () {
        return wallet;
    }

    public void setBidPrice (BigDecimal bidPrice) {
        this.bidPrice = bidPrice;
    }

    public void setAskPrice (BigDecimal askPrice) {
        this.askPrice = askPrice;
    }

    public void stop() {
        TradingParameters.getApi()
                        .getStreamingContext()
                        .sendRequest(StreamingRequest.unsubscribeCandle(instrument.getFigi(), CandleInterval._1MIN));
    }

    public BigDecimal getMaxBidsPrice() {
        return getMaxPrice(bidsList);
    }

    public BigDecimal getMaxAsksPrice() {
        return getMaxPrice(asksList);
    }

    public int getMaxAsksQuantity() {
        return getMaxQuantity(asksList);
    }

    public int getMaxBidsQuantity() {
        return getMaxQuantity(bidsList);
    }

    public BigDecimal getLastPrice() {
        return this.lastPrice;
    }

    public void setLastPrice(BigDecimal lastPrice) {
        this.lastPrice = lastPrice;
    }

    public void setLimitOrder(BigDecimal price, int lot, OperationType opType) {
        LimitOrderRequest ord = new LimitOrderRequest();
        ord.setPrice(price);
        ord.lots(lot);
        ord.operation(opType);
    }

    public void setMarketOrder(int lot, OperationType opType) {
        MarketOrderRequest mord = new MarketOrderRequest();
        mord.lots(lot);
        mord.operation(opType);
    }

    private void makeSell(StreamingEvent.Candle event) {
        if (wallet.compareTo(parameters.value) < 0) {
            return;
        }
        int countInst = TradingParameters.getPortfolio().getPortfolioByFigi(instrument.getFigi()).getLots();
        if (countInst > 0) {
            //    System.out.println(TradingParameters.getPortfolio().getPortfolioByFigi(instrument.getFigi()).getBalance().divide(new BigDecimal(TradingParameters.getPortfolio().getPortfolioByFigi(instrument.getFigi()).getLots())));
            BigDecimal balance = TradingParameters.getPortfolio()
                    .getPortfolioByFigi(instrument.getFigi())
                    .getBalance()
                    .divide(new BigDecimal(TradingParameters.getPortfolio()
                                                            .getPortfolioByFigi(instrument.getFigi())
                                                            .getLots()));
            //    BigDecimal balance = TradingParameters.getPortfolio().getPortfolioByFigi(instrument.getFigi()).getAveragePositionPrice().getValue();
            BigDecimal profit = lastPrice.divide(balance).multiply(new BigDecimal(100));
            TradingParameters.getLogger().info("В портфеле  " + countInst
                    + " лотов с ценой " + balance
                    + " возможная прибыль %" + profit);
            if (profit.compareTo(parameters.profit) >= 0) {
                int value=0;
                if (firstSell) {
                    System.out.println("Это первая продажа " + firstSellWallet);
                    value = parameters.value.divide(lastPrice, 0, 1).intValue();
                    if (value > countInst) value = countInst;
                } else {
                    value = parameters.value.subtract(wallet).divide(lastPrice, 0, 1).intValue();
                }
                if (value > event.getTradingValue().intValue()) {
                    value = event.getTradingValue().intValue() / 2;
                }
                TradingParameters.getLogger().info("Прибыль достаточна, продаем " + value + " лотов");
                setMarketOrder(value, OperationType.SELL);
                BigDecimal price = lastPrice.multiply(new BigDecimal(value));
                wallet = wallet.add(price);
                firstSellWallet = firstSellWallet.add(price);
                if (firstSellWallet.compareTo(parameters.value) >= 0) {
                    firstSell = false;
                }
                if (wallet.compareTo(parameters.value) > 0) {
                    wallet=parameters.value;
                }
                System.out.println("wallet=" + wallet);
                System.out.println(firstSellWallet);
            }
             else {
                TradingParameters.getLogger().info("Прибыль не достаточна ");
            }
        }
    }

    private void makeBuy(StreamingEvent.Candle event) {
        String cur = instrument.getCurrency().getValue();
        getOrderList();
        if (cur.equals("USD")) {
            BigDecimal value;
            value = TradingParameters.getPortfolio().getPortfolioByTicker("USD000UTSTOM").getBalance();
            if (value.compareTo(parameters.value) > 0) {
                value = parameters.value;
            }
            if (getPlanProfit().compareTo(parameters.profit) >= 0) {
                BigDecimal count = value.divide(getLastPrice(), 0, 3);
                if (count.compareTo(event.getTradingValue()) > 0) {
                    count = event.getTradingValue().divide(new BigDecimal(1));
                }
                TradingParameters.getLogger().info("Позиция " + instrument.getName()
                        + " Цена покупки " + lastPrice
                        + "; Цена продажи " + askPrice
                        + "; Прибыльность операции " + getPlanProfit()
                        + " Берем " + count);
                setMarketOrder(count.intValue(), OperationType.BUY);
                BigDecimal price = lastPrice.multiply( new BigDecimal(count.intValue()) );
                wallet = wallet.subtract(price);
                System.out.println(wallet);
            } else {
                TradingParameters.getLogger().info("Позиция " + instrument.getName()
                        + " Прибыльность " + getPlanProfit()
                        + " ниже плановой " + parameters.profit);
            }
        }
    }

    public void analize(StreamingEvent.Candle event) {
        if (event.getFigi().compareTo(this.instrument.getFigi()) == 0) {
            lastPrice = event.getClosingPrice();
            lastQuantity = event.getTradingValue().intValue();
            makeSell(event);
            makeBuy(event);
        }
    }

    public BigDecimal getPlanProfit() {
        int round = 2; //Точность расчета комисии
        BigDecimal planProfit;
        BigDecimal commition = new BigDecimal("0.003");
        BigDecimal lastPriceComm = lastPrice.add(lastPrice.multiply(commition)).setScale(round, RoundingMode.HALF_UP);
        BigDecimal askPriceComm = askPrice.subtract(askPrice.multiply(commition)).setScale(round, RoundingMode.HALF_UP);
//        System.out.println(lastPriceComm);
//        System.out.println(askPriceComm);
//        planProfit = BigDecimal.valueOf(1).subtract(lastPrice.divide(bidPrice)).multiply(new BigDecimal(100));
//            multiply(askPrice.subtract(bidPrice).subtract(askPrice.multiply(commition).setScale(round, RoundingMode.HALF_UP).add(bidPrice.multiply(commition).setScale(round, RoundingMode.HALF_UP)))).
//                multiply(bidPrice);
        planProfit = BigDecimal.valueOf(1)
                .subtract( lastPriceComm.divide(askPriceComm, 3, RoundingMode.HALF_UP) )
                .multiply(new BigDecimal(100));
//        System.out.println(planProfit);
        return planProfit;
    }

    private void getOrderList() {
        final var order = TradingParameters.getApi().getMarketContext()
                .getMarketOrderbook(instrument.getFigi(), 50).join();
        bidsList = order.get().getBids().toArray(new OrderResponse[0]);
        asksList = order.get().getAsks().toArray(new OrderResponse[0]);
    }

    private static int getMax(OrderResponse[] bids) {
        int max = 0;
        int maxCount = 0;
        for (int count = 0; count < bids.length; count++) {
            if (max < bids[count].getQuantity()) {
                max = bids[count].getQuantity();
                maxCount = count;
            }
        }
        return maxCount;
    }

    private static int getMaxQuantity(OrderResponse[] list) {
        return list[getMax(list)].getQuantity();
    }

    private static BigDecimal getMaxPrice(OrderResponse[] list) {
        int count = getMax(list);
        if (count == 0) {
            return new BigDecimal(0);
        }
        return list[count].getPrice();
    }

    public MarketInstrument getInstrument () {
        return instrument;
    }
}
