package io.reactivex.netty.protocol.http.server;

import io.netty.handler.codec.http.HttpResponseStatus;
import rx.Observable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRouter<I, O> implements RequestHandler<I, O> {
    private final List<PatternBinding> getBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> putBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> postBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> deleteBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> optionsBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> headBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> traceBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> connectBindings = new CopyOnWriteArrayList<PatternBinding>();
    private final List<PatternBinding> patchBindings = new CopyOnWriteArrayList<PatternBinding>();
    private RequestHandler<I, O> noMatchHandler;

    @Override
    public Observable<Void> handle(HttpServerRequest<I> request, HttpServerResponse<O> response) {
        String method = request.getHttpMethod().toString();

        if("GET".equals(method))
            route(request, response, getBindings);
        else if("PUT".equals(method))
            route(request, response, putBindings);
        else if("POST".equals(method))
            route(request, response, postBindings);
        else if("DELETE".equals(method))
            route(request, response, deleteBindings);
        else if("OPTIONS".equals(method))
            route(request, response, optionsBindings);
        else if("HEAD".equals(method))
            route(request, response, headBindings);
        else if("TRACE".equals(method))
            route(request, response, traceBindings);
        else if("PATCH".equals(method))
            route(request, response, patchBindings);
        else if("CONNECT".equals(method))
            route(request, response, connectBindings);
        else
            notFound(request, response);

        if(!response.isCloseIssued()) // might have been issued by an overzealous handler
            return response.close();
        return Observable.empty();
    }

    public HttpRouter<I, O> get(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, getBindings);
        return this;
    }

    public HttpRouter<I, O> put(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, putBindings);
        return this;
    }

    public HttpRouter<I, O> post(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, postBindings);
        return this;
    }

    public HttpRouter<I, O> delete(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, deleteBindings);
        return this;
    }

    public HttpRouter<I, O> options(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, optionsBindings);
        return this;
    }

    public HttpRouter<I, O> head(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, headBindings);
        return this;
    }

    public HttpRouter<I, O> trace(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, traceBindings);
        return this;
    }

    public HttpRouter<I, O> connect(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, connectBindings);
        return this;
    }

    public HttpRouter<I, O> patch(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, patchBindings);
        return this;
    }

    /**
     * Specify a handler that will be called for all HTTP methods
     * @param pattern The simple pattern
     * @param handler The handler to call
     */
    public HttpRouter<I, O> all(String pattern, RequestHandler<I, O> handler) {
        addPattern(pattern, handler, getBindings);
        addPattern(pattern, handler, putBindings);
        addPattern(pattern, handler, postBindings);
        addPattern(pattern, handler, deleteBindings);
        addPattern(pattern, handler, optionsBindings);
        addPattern(pattern, handler, headBindings);
        addPattern(pattern, handler, traceBindings);
        addPattern(pattern, handler, connectBindings);
        addPattern(pattern, handler, patchBindings);
        return this;
    }

    public HttpRouter<I, O> getWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, getBindings);
        return this;
    }

    public HttpRouter<I, O> putWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, putBindings);
        return this;
    }

    public HttpRouter<I, O> postWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, postBindings);
        return this;
    }

    public HttpRouter<I, O> deleteWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, deleteBindings);
        return this;
    }

    public HttpRouter<I, O> optionsWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, optionsBindings);
        return this;
    }

    public HttpRouter<I, O> headWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, headBindings);
        return this;
    }

    public HttpRouter<I, O> traceWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, traceBindings);
        return this;
    }

    public HttpRouter<I, O> connectWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, connectBindings);
        return this;
    }

    public HttpRouter<I, O> patchWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, patchBindings);
        return this;
    }

    public HttpRouter<I, O> allWithRegEx(String regex, RequestHandler<I, O> handler) {
        addRegEx(regex, handler, getBindings);
        addRegEx(regex, handler, putBindings);
        addRegEx(regex, handler, postBindings);
        addRegEx(regex, handler, deleteBindings);
        addRegEx(regex, handler, optionsBindings);
        addRegEx(regex, handler, headBindings);
        addRegEx(regex, handler, traceBindings);
        addRegEx(regex, handler, connectBindings);
        addRegEx(regex, handler, patchBindings);
        return this;
    }

    public HttpRouter<I, O> noMatch(RequestHandler<I, O> handler) {
        noMatchHandler = handler;
        return this;
    }

    private void addPattern(String input, RequestHandler<I, O> handler, List<PatternBinding> bindings) {
        // We need to search for any :<token name> tokens in the String and replace them with named capture groups
        Matcher m =  Pattern.compile(":([A-Za-z][A-Za-z0-9_]*)").matcher(input);
        StringBuffer sb = new StringBuffer();
        Set<String> groups = new HashSet<String>();
        while (m.find()) {
            String group = m.group().substring(1);
            if (groups.contains(group)) {
                throw new IllegalArgumentException("Cannot use identifier " + group + " more than once in pattern string");
            }
            m.appendReplacement(sb, "(?<$1>[^\\/]+)");
            groups.add(group);
        }
        m.appendTail(sb);
        String regex = sb.toString();
        PatternBinding binding = new PatternBinding(Pattern.compile(regex), groups, handler);
        bindings.add(binding);
    }

    private void addRegEx(String input, RequestHandler<I, O> handler, List<PatternBinding> bindings) {
        PatternBinding binding = new PatternBinding(Pattern.compile(input), null, handler);
        bindings.add(binding);
    }

    private void route(HttpServerRequest<I> request, HttpServerResponse<O> response, List<PatternBinding> bindings) {
        for (PatternBinding binding: bindings) {
            Matcher m = binding.pattern.matcher(request.getPath()); // FIXME what is the difference between path and uri?
            if (m.matches()) {
                if (binding.paramNames != null) {
                    // Named params
                    for (String param: binding.paramNames)
                        addQueryParam(request, param, m.group(param));
                } else {
                    // Un-named params
                    for (int i = 0; i < m.groupCount(); i++)
                        addQueryParam(request, "param" + i, m.group(i+1));
                }
                binding.handler.handle(request, response);
                return;
            }
        }
        notFound(request, response);
    }

    private void addQueryParam(HttpServerRequest<I> request, String key, String val) {
        List<String> params = request.getQueryParameters().get(key);
        if(params == null) {
            params = new ArrayList<String>();
            request.getQueryParameters().put(key, params);
        }
        params.add(val);
    }

    private void notFound(HttpServerRequest<I> request, HttpServerResponse<O> response) {
        if (noMatchHandler != null)
            noMatchHandler.handle(request, response);
        else
            response.setStatus(HttpResponseStatus.NOT_FOUND);
    }

    private class PatternBinding {
        final Pattern pattern;
        final RequestHandler<I, O> handler;
        final Set<String> paramNames;

        private PatternBinding(Pattern pattern, Set<String> paramNames, RequestHandler<I, O> handler) {
            this.pattern = pattern;
            this.handler = handler;
            this.paramNames = paramNames;
        }
    }
}
