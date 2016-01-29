/*
 * Copyright 2016 Ignacio del Valle Alles idelvall@brutusin.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.brutusin.rpc;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import org.apache.catalina.Globals;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.brutusin.rpc.actions.websocket.PublishAction;
import org.brutusin.rpc.http.HttpAction;
import org.brutusin.rpc.spi.ServerRuntime;
import org.brutusin.rpc.websocket.Topic;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class TomcatRuntime extends ServerRuntime {

    private static StandardContext addTestApp(final Tomcat tomcat, final String... openUrl) throws ServletException {
        StandardContext ctx = (StandardContext) tomcat.addWebapp("/", new File("").getAbsolutePath());
        File additionWebInfClasses = new File("target/classes");
        WebResourceRoot resources = new StandardRoot(ctx);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
        ctx.addApplicationListener(RpcInitListener.class.getName());

        ctx.setResources(resources);
        if (openUrl != null) {
            ctx.addApplicationLifecycleListener(new ServletContextListener() {

                public void contextInitialized(ServletContextEvent sce) {
                    if (Desktop.isDesktopSupported()) {
                        try {
                            for (int i = 0; i < openUrl.length; i++) {
                                Desktop.getDesktop().browse(new URI(openUrl[i]));
                            }
                        } catch (Exception ex) {
                            Logger.getLogger(TomcatRuntime.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

                public void contextDestroyed(ServletContextEvent sce) {
                    try {
                        tomcat.stop();
                    } catch (LifecycleException ex) {
                        Logger.getLogger(TomcatRuntime.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            });
        }
        return ctx;
    }

    private static Tomcat createTomcat(int port) throws IOException {
        System.setProperty("org.apache.catalina.startup.EXIT_ON_INIT_FAILURE", "true");
        Tomcat tomcat = new Tomcat();
        Path tempPath = Files.createTempDirectory("brutusin-rcp-tests");
        tomcat.setBaseDir(tempPath.toString());
        tomcat.setPort(port);
        return tomcat;
    }

    private static class Tomcat extends org.apache.catalina.startup.Tomcat {

        public int getPort() {
            return port;
        }
    }

    @Override
    public String getURIEncoding() {
        if (Globals.STRICT_SERVLET_COMPLIANCE) {
            return "ISO-8859-1";
        } else {
            return "UTF-8";
        }
    }

    @Override
    public void exec(int port) {
        try {
            RpcContextImpl.testMode = false;
            Tomcat tomcat = createTomcat(port);
            addTestApp(tomcat, "http://localhost:" + port + "/rpc/repo/");
            tomcat.start();
            tomcat.getServer().await();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void test(int port, RpcAction action) {
        try {
            RpcContextImpl.testMode = true;
            Tomcat tomcat = createTomcat(port);
            String id = action.getClass().getName();
            String url;
            if (action instanceof HttpAction) {
                url = "http://localhost:" + tomcat.getPort() + "/rpc/repo/#http-services/" + id;
            } else {
                url = "http://localhost:" + tomcat.getPort() + "/rpc/repo/#wskt-services/" + id;
            }
            RpcContext.getInstance().register(id, action);

            addTestApp(tomcat, url);
            tomcat.start();
            tomcat.getServer().await();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void test(int port, final Topic topic) {
        try {
            RpcContextImpl.testMode = true;
            Tomcat tomcat = createTomcat(port);
            String topicId = topic.getClass().getName();
            PublishAction publishAction = new PublishAction(topic);
            RpcContext.getInstance().register("publish-service", publishAction);
            RpcContext.getInstance().register(topicId, topic);
            addTestApp(tomcat, "http://localhost:" + port + "/rpc/test/topic.jsp?id=" + topicId);
            tomcat.start();
            tomcat.getServer().await();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
