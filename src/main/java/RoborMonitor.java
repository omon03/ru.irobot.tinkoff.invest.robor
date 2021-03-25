import ru.tinkoff.invest.openapi.model.rest.MarketInstrument;
import ru.tinkoff.invest.openapi.model.streaming.StreamingEvent;

public class RoborMonitor {
    Robor robor;
    TradingParameters parameters;

    public RoborMonitor (TradingParameters parameters)
     {
        this.parameters = parameters;
     }

     void event (StreamingEvent event) {
         TradingParameters.getLogger().info("Полученио сообщение "+event);
        if(event.getClass()== StreamingEvent.Candle.class) {
            TradingParameters.getLogger().info("Сообщение содержит информацию о свечах");
            StreamingEvent.Candle candle = (StreamingEvent.Candle) event;
            if (candle.getFigi().compareTo(robor.getInstrument().getFigi()) == 0) {
                robor.analize(candle);
            } else {TradingParameters.getLogger().info("Не подходящий инструмент");}
        }
     }

     void addRobor(MarketInstrument instrument) {
         robor = new Robor(instrument,parameters,parameters.value);
     }

     void start() {
        if(robor!=null) {
            robor.start();
        }
     }

}
