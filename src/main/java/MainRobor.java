import io.reactivex.rxjava3.core.Flowable;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.model.rest.MarketInstrument;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.concurrent.CompletableFuture;


public class MainRobor {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        TradingParameters parameters = new TradingParameters();
        ParametersWindow dialog;// = new ParametersWindow(parameters);
 //       String ticker=parameters.ticker.get(0);
        try {
            parameters.open();
        } catch ( NullPointerException | InvalidClassException | FileNotFoundException ex) {
      //      System.out.println(ex);
            dialog = new ParametersWindow(parameters);
            dialog.pack();
            dialog.setVisible(true);
        }
        MarketInstrument instrument;
        parameters.connApi();
        try {
            instrument = getInstrument(TradingParameters.getApi(),removeKv(parameters.ticker.get(0)));
            RoborMonitor rm = new RoborMonitor(parameters);
            rm.addRobor(instrument);
            rm.start();

            final var stopNotifier = new CompletableFuture<Void>();
            final var rxStreaming = Flowable.fromPublisher(TradingParameters.getApi().getStreamingContext());
            final var rxSubscription = rxStreaming
                    .doOnError(stopNotifier::completeExceptionally)
                    .doOnComplete(() -> stopNotifier.complete(null))
                    .forEach(rm::event);
        } catch (final IllegalArgumentException ex) {
            TradingParameters.getLogger().error("Ошибка тикера ", ex);
        }
    }

    static MarketInstrument getInstrument(OpenApi api, String ticker) {
        MarketInstrument instrument;
        final var instrumentOpt = api.getMarketContext()
                .searchMarketInstrumentsByTicker(ticker)
                .join()
                .getInstruments()
                .stream()
                .findFirst();
        if (instrumentOpt.isEmpty()) {
            throw new IllegalArgumentException("Нет инструмента с таким тикером");
        }
        return instrumentOpt.get();
    }

    static String removeKv(String st) {
        return st.substring(1, st.length() - 1);
    }
}
