import ru.tinkoff.invest.openapi.model.rest.Order;
import ru.tinkoff.invest.openapi.model.rest.PortfolioPosition;

import java.util.List;
import java.util.Objects;

public class  RoborPortfolio {
    TradingParameters parameters;
    PortfolioPosition [] myPortfolio;
    List<Order> myOrders;

    public RoborPortfolio (TradingParameters parameters) {
        this.parameters = parameters;
        update();
    }

    public void update() {
        var currentPositions = parameters.getApi().getPortfolioContext()
                .getPortfolio(null).join();
        int count = currentPositions.getPositions().size();
        myPortfolio = new PortfolioPosition[count];
        for (int i = 0; i < count; i++) {
            myPortfolio[i]=currentPositions.getPositions().get(i);
        }
        myOrders = parameters.getApi().getOrdersContext().getOrders(null).join();
    }

    public PortfolioPosition getPortfolioByTicker(String ticker) {
        int i = 0;
        for (; i < myPortfolio.length; i++) {
            if ( ticker.equals(myPortfolio[i].getTicker()) ) {
                return myPortfolio[i];
            }
        }
        return  myPortfolio[i - 1];
    }

    public PortfolioPosition getPortfolioByFigi(String figi) {
        int i = 0;
        for (; i < myPortfolio.length; i++) {
            if (figi.equals(myPortfolio[i].getFigi())) {
                return myPortfolio[i];
            }
        }
        return  myPortfolio[i - 1];
    }

    public int getPortfolioSize() {
        return myPortfolio.length;
    }

    public int getOrdersSize() {
        return myOrders.size();
    }

    public Order getOrderByID(int id) {
        return myOrders.get(id);
    }

    public Order getOrderByFigi(String figi) {
        int i = 1;
        for (; i < myOrders.size(); i++) {
            if ( Objects.equals(figi, myOrders.get(i).getFigi()) ) {
                return myOrders.get(i);
            }
        }
        return myOrders.get(i - 1);
    }

    public String toString() {
        StringBuilder ex = new StringBuilder("RoborPortfolio {" + '\n');
        for (PortfolioPosition portfolioPosition : myPortfolio) {
            ex.append('\t')
                .append(portfolioPosition.getName())
                .append("=")
                .append(portfolioPosition.getBalance())
                .append(", ")
                .append(portfolioPosition.getLots())
                .append(",")
                .append('\n');
        }
        ex.append("}");

        return ex.toString();
    }
}
