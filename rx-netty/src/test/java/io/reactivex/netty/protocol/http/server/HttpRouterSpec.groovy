package io.reactivex.netty.protocol.http.server

import io.netty.handler.codec.http.HttpMethod
import spock.lang.Ignore
import spock.lang.Specification

class HttpRouterSpec extends Specification {
    def 'router matches by method and path'() {
        setup:
        def request = Mock(HttpServerRequest)
        def response = Mock(HttpServerResponse)

        when:
        new HttpRouter().get('/endpoint', { req, resp -> resp.write('hit') } as RequestHandler).handle(request, response)

        then:
        request.httpMethod >> method
        request.path >> path
        hits * response.write('hit')

        where:
        method      |   path            |   hits
        HttpMethod.GET         |   '/endpoint'     |   1
        HttpMethod.GET         |   '/unmapped'     |   0
        HttpMethod.POST        |   '/endpoint'     |   0
    }

    def 'router matches by method and regex'() {
        setup:
        def request = Mock(HttpServerRequest)
        def response = Mock(HttpServerResponse)

        when:
        new HttpRouter().get('/a+', { req, resp -> resp.write('hit') } as RequestHandler).handle(request, response)

        then:
        request.httpMethod >> method
        request.path >> path
        hits * response.write('hit')

        where:
        method      |   path            |   hits
        HttpMethod.GET         |   '/a'            |   1
        HttpMethod.GET         |   '/aa'           |   1
        HttpMethod.GET         |   '/ab'           |   0
        HttpMethod.POST        |   '/a'            |   0
    }

    def 'router extracts request parameters from the path'() {
        setup:
        def request = Mock(HttpServerRequest)
        def response = Mock(HttpServerResponse)
        request.httpMethod >> HttpMethod.GET

        def params = [:]
        request.queryParameters >> params

        when:
        new HttpRouter().get('/:blogname/:post', { req, resp ->
            resp.write(req.queryParameters['blogname'])
            resp.write(req.queryParameters['post'])
        } as RequestHandler).handle(request, response)

        then:
        request.getPath() >> "/blog/post"
        1 * response.write(['blog'])
        1 * response.write(['post'])

        then:
        request.getPath() >> "/blog"
        0 * response.write(_)
    }
}
