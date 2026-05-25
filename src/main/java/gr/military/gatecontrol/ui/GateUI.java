package gr.military.gatecontrol.ui;

import gr.military.gatecontrol.service.GateAccessService;
import gr.military.gatecontrol.service.MovementService;
import gr.military.gatecontrol.service.PersonService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class GateUI extends Application {

    private static ConfigurableApplicationContext springContext;

    public static void setContext(ConfigurableApplicationContext context) {
        springContext = context;
        GateUIWithRoster.setServices(
                context.getBean(GateAccessService.class),
                context.getBean(MovementService.class),
                context.getBean(PersonService.class)
        );
        GateUIWithRoster.setDataSource(context.getBean(javax.sql.DataSource.class));
    }

    @Override
    public void start(Stage stage) throws Exception {
        stage.setOnCloseRequest(e -> shutdown());
        new GateUIWithRoster().start(stage);
    }

    @Override
    public void stop() {
        shutdown();
    }

    private void shutdown() {
        try {
            if (springContext != null) {
                SpringApplication.exit(springContext);
                springContext.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        Platform.exit();
        System.exit(0);
    }
}