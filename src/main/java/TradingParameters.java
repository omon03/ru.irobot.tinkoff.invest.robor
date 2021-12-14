import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.tinkoff.invest.openapi.OpenApi;
import ru.tinkoff.invest.openapi.model.rest.SandboxRegisterRequest;
import ru.tinkoff.invest.openapi.okhttp.OkHttpOpenApi;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.LogManager;

public class TradingParameters  implements Serializable {
    private static transient org.slf4j.Logger logger;
    private static transient OpenApi api;
    private static transient RoborPortfolio portfolio;
    private static String fileName = "..//irobor.cfg";
    public String ssoKey;
    public boolean sandBox = true;
    public BigDecimal profit = new BigDecimal(2.3).setScale(2, RoundingMode.HALF_UP);
    public BigDecimal value = new BigDecimal(10);
    public BigDecimal commission = new BigDecimal(0.003).setScale(3, RoundingMode.HALF_UP);
    public ArrayList<String> ticker = new ArrayList<>();
    public boolean silentMode = true;

    public TradingParameters() throws IOException, ClassNotFoundException  {
        try {
            logger = initLogger();
        } catch (IOException ex) {
            System.err.println("При инициализации логгера произошла ошибка: " + ex.getLocalizedMessage());
        }
        ticker.add("veon");
    }
    public static org.slf4j.Logger getLogger() {
        return logger;
    }

    public static OpenApi getApi() {
        return  api;
    }

    public static RoborPortfolio getPortfolio() {
        return portfolio;
    }

    public void open() throws IOException, ClassNotFoundException {
        TradingParameters param = new TradingParameters();

        try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(fileName))) {
            try {
                logger.info("Читаем параметры из файла");
                param = (TradingParameters) objectInputStream.readObject();
                objectInputStream.close();
            } catch (InvalidClassException ex) {
                throw new InvalidClassException("Из файла получены не верные данные "+ex);
            }
            ssoKey = param.ssoKey;
            value = param.value;
            sandBox = param.sandBox;
            profit = param.profit;
            commission = param.commission;
            ticker = param.ticker;
            logger.info("Данные прочитаны " + this);
            if ((ssoKey == null)) {
                throw new NullPointerException("Не указан ключ. выполнение не возможно");
            }
        } catch (final FileNotFoundException ex) {
            throw new FileNotFoundException("Нет Такого файла "+fileName);
        }
    }

    public void connApi() throws IOException {
        if(api != null) {
            logger.info("Подключение уже создано ");
        } else {
            if(ssoKey != null) {
                try {
                    api = new OkHttpOpenApi(ssoKey, sandBox);
                    logger.info("Создаём подключение... ");
                    if (api.isSandboxMode()) {
                        api.getSandboxContext().performRegistration(new SandboxRegisterRequest()).join();
                    }
                    portfolio = new RoborPortfolio(this);
                    logger.info("подключение успешно");
                } catch (Exception ex) {
                    logger.error("Ошибка подключения, перезапустите приложение для настройки ", ex);
                    ssoKey = null;
                    save();
                    System.exit(0);
                }
            } else {
                logger.error("ssoKey пуст. завершаем работу");
                System.exit(0);
            }
        }
    }

    public  void closeApi() throws IOException {
    //    api.getStreamingContext().sendRequest(StreamingRequest.unsubscribeCandle());

        logger.info("Закрываем подключение ");
        try{
            api.close();
        }
        catch (final IOException | NullPointerException ex) {
            logger.error("Ошибка закрытия подключения ",ex);
        }
    }

    public void save() throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName));
        objectOutputStream.writeObject(this);
        objectOutputStream.close();
    }

    public String toString () {
        return "TradingParameters{" +
                "ssoKey='" + ssoKey + '\'' +
                ", sandBox=" + sandBox +'\''+
                ", profit=" + profit +'\''+
                ", value=" + value +'\''+
                ", commission=" + commission +'\''+
                ", ticker=" + ticker +'\''+
                '}';
    }

    private static Logger initLogger() throws IOException {
        final var logManager = LogManager.getLogManager();
        final var classLoader = MainRobor.class.getClassLoader();

        try (final InputStream input = classLoader.getResourceAsStream("logging.properties")) {

            if (input == null) {
                throw new FileNotFoundException();
            }

            Files.createDirectories(Paths.get("./logs"));
            logManager.readConfiguration(input);
        }

        return LoggerFactory.getLogger(MainRobor.class);
    }
}
