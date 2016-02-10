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
package org.brutusin.rpc.http;

import java.security.Principal;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.brutusin.rpc.SpringContextImpl;
import org.brutusin.rpc.websocket.Topic;
import org.brutusin.rpc.websocket.WebsocketAction;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 *
 * @author Ignacio del Valle Alles idelvall@brutusin.org
 */
public class HttpActionSupportImpl extends HttpActionSupport {

    private final SpringContextImpl springCtx;
    private final HttpServletRequest req;
    private final HttpServletResponse resp;

    public static void setInstance(HttpActionSupport context) {
        CONTEXTS.set(context);
    }

    public static void clear() {
        CONTEXTS.remove();
    }

    public HttpActionSupportImpl(SpringContextImpl springCtx) {
        this(springCtx, null, null);
    }

    public HttpActionSupportImpl(HttpServletRequest req, HttpServletResponse resp) {
        this((SpringContextImpl) WebApplicationContextUtils.getWebApplicationContext(req.getServletContext()), req, resp);
    }

    private HttpActionSupportImpl(SpringContextImpl springCtx, HttpServletRequest req, HttpServletResponse resp) {
        this.springCtx = springCtx;
        this.req = req;
        this.resp = resp;
    }

    @Override
    public HttpServletRequest getRequest() {
        return req;
    }

    @Override
    public HttpServletResponse getResponse() {
        return resp;
    }

    @Override
    public final Principal getUserPrincipal() {
        if (req == null) {
            throw new IllegalStateException("Trying to get principal out of the context of a request");
        }
        return req.getUserPrincipal();
    }

    @Override
    public final boolean isUserInRole(String role) {
        if (req == null) {
            throw new IllegalStateException("Trying to get principal role out of the context of a request");
        }
        return req.isUserInRole(role);
    }

    public SpringContextImpl getSpringContextImpl() {
        return springCtx;
    }

    @Override
    public ApplicationContext getSpringContext() {
        return getSpringContextImpl();
    }

    @Override
    public Map<String, HttpAction> getHttpServices() {
        return getSpringContextImpl().getHttpServices();
    }

    @Override
    public Map<String, WebsocketAction> getWebSocketServices() {
        return getSpringContextImpl().getWebSocketServices();
    }

    @Override
    public Map<String, Topic> getTopics() {
        return getSpringContextImpl().getTopics();
    }
}
