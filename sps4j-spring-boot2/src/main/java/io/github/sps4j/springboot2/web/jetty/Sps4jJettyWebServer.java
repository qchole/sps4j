package io.github.sps4j.springboot2.web.jetty;

import lombok.SneakyThrows;
import org.eclipse.jetty.server.AbstractNetworkConnector;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;

import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;

public class Sps4jJettyWebServer implements WebServer {
    private final Server jetty;
    private final Handler handler;
    private final ContextHandlerCollection handlerCollection;
    public Sps4jJettyWebServer(Server server, Handler handler, ContextHandlerCollection handlerCollection) {
        this.jetty = server;
        this.handler = handler;
        this.handlerCollection = handlerCollection;
        initialize();
    }

    @SneakyThrows
    void initialize() {
        handler.start();
    }

    @Override
    @SneakyThrows
    public void start() throws WebServerException {
        //do nothing
    }



    @Override
    @SneakyThrows
    public void stop() {
        handlerCollection.removeHandler(handler);
        handler.stop();
    }

    @Override
    public int getPort() {
        Connector[] connectors = this.jetty.getConnectors();
        if (connectors.length != 0) {
            Connector connector = connectors[0];
            if (connector instanceof AbstractNetworkConnector) {
                return ((AbstractNetworkConnector) connector).getLocalPort();
            }
        }
        return 0;
    }

}
