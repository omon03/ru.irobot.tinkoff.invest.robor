import ru.tinkoff.invest.openapi.model.rest.Order;
import ru.tinkoff.invest.openapi.model.rest.PortfolioPosition;

import java.util.List;

public class  RoborPortfolio {
    TradingParameters parameters;
    PortfolioPosition [] myPortfolio;
    List<Order> myOrders;

    public RoborPortfolio (TradingParameters parameters) {
        this.parameters=parameters;
        update();
    }

    public void update() {
        var currentPositions = parameters.getApi().getPortfolioContext().getPortfolio(null).join();
        int count = currentPositions.getPositions().size();
        myPortfolio = new PortfolioPosition[count];
        for(int i=0;i<count;i++) {
            myPortfolio[i]=currentPositions.getPositions().get(i);
        }
        myOrders = parameters.getApi().getOrdersContext().getOrders(null).join();
    }

    public PortfolioPosition getPortfolioByTicker (String ticker) {
        int i;
        for(i=0;i< myPortfolio.length;i++) {
            if(ticker.equals(myPortfolio[i].getTicker())) return myPortfolio[i];
        }
        return  myPortfolio[i];

        }

    public PortfolioPosition getPortfolioByFigi(String figi) {
        int i;
        for(i=0;i< myPortfolio.length-1;i++) {
           if(figi.equals(myPortfolio[i].getFigi())) return myPortfolio[i];
        }
       return  myPortfolio[i];
    };

    public int getPortfolioSize () {
        return myPortfolio.length;
    }

    public int getOrdersSize () {return myOrders.size(); }

    public Order getOrderByID(int id) {return myOrders.get(id);}

    public Order getOrderByFigi(String figi) {
        int i;
        for(i=1;i<myOrders.size();i++) {
            if(figi==myOrders.get(i).getFigi()) {return myOrders.get(i);}
        }
        return myOrders.get(i);
    }

    public String toString() {
        String ex = "RoborPortfolio {"+'\n';
        for(int i=0;i< myPortfolio.length;i++) {
            ex=ex+'\t'+myPortfolio[i].getName()+"="+myPortfolio[i].getBalance()+", "+myPortfolio[i].getLots()+","+'\n';
        }
        ex=ex+"}";

        return ex;
    }
}
