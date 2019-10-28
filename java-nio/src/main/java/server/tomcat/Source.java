package server.tomcat;

import org.apache.catalina.LifecycleException;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.junit.Test;

public class Source {

    @Test
    public void test() throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        Connector connector = new Connector("HTTP/1.1");
        connector.setPort(8080);
        tomcat.setConnector(connector);
        tomcat.start();
        tomcat.getServer().await();
    }
}
