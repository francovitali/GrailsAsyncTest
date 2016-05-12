package com.asyncTest

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import grails.converters.JSON
import groovyx.gpars.dataflow.Promise
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.conn.ConnectionPoolTimeoutException
import org.apache.http.conn.routing.HttpRoute

import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException

import static groovyx.gpars.GParsPool.withPool
import static groovyx.gpars.dataflow.Dataflow.whenAllBound
import static groovyx.gpars.dataflow.Dataflow.task
import groovyx.net.http.AsyncHTTPBuilder

import org.apache.http.params.HttpConnectionParams
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder

import org.apache.http.protocol.HttpContext
import org.apache.http.client.protocol.HttpClientContext

import org.apache.http.HttpResponse
import org.apache.http.client.methods.CloseableHttpResponse

import org.apache.http.client.HttpClient
import org.apache.http.nio.client.HttpAsyncClient

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient

import com.google.gson.Gson

//import grails.async.Promise
//import static grails.async.Promises.task
//import static grails.async.Promises.waitAll

import java.util.concurrent.Future

class TestController {

    private static Gson gson = new Gson();

    UserService userService
    HttpClient httpClient
    HttpAsyncClient httpAsyncClient

    @UserRequired(true)
    def index() {
        render([message: 'The user is NOT required'] as JSON)
    }

    def slowAction(int seconds) {
        println "slowAction ($seconds)"
        Thread.sleep(seconds * 1000)
        render([message: "ok ($seconds)"] as JSON)
    }

    def cnnTo(int seconds) {
        try {
            def some = userService.getSome(seconds)
            render some
        } catch (Exception ex) {
            println ex.getClass()
            throw ex
        }
    }

    def cnnSync() {
        HttpContext httpContext = HttpClientContext.create()
        HttpGet httpGet = new HttpGet("http://localhost:8080/AsyncTest/Test/slowAction?seconds=2")
        CloseableHttpClient httpClient = HttpClients.createDefault()
        try {
            CloseableHttpResponse httpResponse = httpClient.execute(httpGet, httpContext)

            int status = httpResponse.getStatusLine().getStatusCode()
            println "**** $status"

            InputStream inputStream = httpResponse.getEntity().getContent()
            JsonObject json = getJSON(inputStream)

            render([status: status, message: json.get("message").getAsString()] as JSON)
        } catch (ConnectionPoolTimeoutException connectionPoolTimeoutException) {
            render 'ConnectionPoolTimeoutException'
        } finally {
            httpClient.close()
        }
    }

    def cnnAsync() {
    }

    def cnnPooled() {
        HttpContext httpContext = HttpClientContext.create()
        HttpGet httpGet = new HttpGet("http://localhost:8080/AsyncTest/Test/slowAction?seconds=2")
        CloseableHttpResponse httpResponse

        try {
            httpResponse = httpClient.execute(httpGet, httpContext)

            int status = httpResponse.getStatusLine().getStatusCode()
            println "**** $status"

            InputStream inputStream = httpResponse.getEntity().getContent()
            JsonObject json = getJSON(inputStream)

            render([status: status, message: json.get("message").getAsString()] as JSON)
        } catch (ConnectionPoolTimeoutException connectionPoolTimeoutException) {
            render 'ConnectionPoolTimeoutException'
        } finally {
            if (httpResponse) {
                httpResponse.close()
            }
        }
    }

    def cnnAsyncPooled() {
        HttpContext httpContext = HttpClientContext.create()
        HttpGet httpGet = new HttpGet("http://localhost:8080/AsyncTest/Test/slowAction?seconds=2")

        try {
            Future<HttpResponse> httpResponseFuture = httpAsyncClient.execute(httpGet, httpContext, null)
            HttpResponse httpResponse = httpResponseFuture.get()

            int status = httpResponse.getStatusLine().getStatusCode()
            println "**** $status"

            InputStream inputStream = httpResponse.getEntity().getContent()
            JsonObject json = getJSON(inputStream)

            render([status: status, message: json.get("message").getAsString()] as JSON)
        } catch (TimeoutException timeoutException) {
            render 'Pool TimeoutException'
        } catch (ExecutionException executionException) {
            if (executionException.getCause() instanceof TimeoutException) {
                TimeoutException timeoutException = executionException.getCause()
                render "ExcecutionException: ${timeoutException}"
            } else {
                throw executionException
            }
        } catch (ConnectionPoolTimeoutException connectionPoolTimeoutException) {
            render 'ConnectionPoolTimeoutException'
        } finally {
        }
    }

    def connectionTimeoutTest() {
        Map<String, Object> params = new HashMap<String, Object>();
        int poolSize = 1
        String baseUri = 'http://localhost:8080'
        int connectionTimeout = 100
        int socketTimeout = 20000

        params.put("poolSize", poolSize);
        params.put("uri", baseUri);
        params.put("contentType", groovyx.net.http.ContentType.JSON);

        AsyncHTTPBuilder asyncHttpBuilder = new AsyncHTTPBuilder(params);

        HttpConnectionParams.setConnectionTimeout(asyncHttpBuilder.getClient().getParams(), connectionTimeout);
        HttpConnectionParams.setSoTimeout(asyncHttpBuilder.getClient().getParams(), socketTimeout);

        def myTask1 = task {
            println 'Executing myTask1'
            Future<String> myPromise1 = asyncHttpBuilder.get(path: '/AsyncTest/Test/slowAction', query: [seconds: 10]) { resp, json ->
                json.message
            }
            myPromise1.get()
        }

        def myTask2 = task {
            println 'Executing myTask2'
            try {
                Future<String> myPromise2 = asyncHttpBuilder.get(path: '/AsyncTest/Test/slowAction', query: [seconds: 5]) { resp, json ->
                    json.message
                }
                myPromise2.get()
            } catch (Throwable t) {
                println t.getClass()
                throw t
            }
        }

        def myTask3 = task {
            println 'Executing myTask3'
            try {
                Future<String> myPromise3 = asyncHttpBuilder.get(path: '/AsyncTest/Test/slowAction', query: [seconds: 1]) { resp, json ->
                    json.message
                }
                myPromise3.get()
            } catch (Throwable t) {
                println t.getClass()
                throw t
            }
        }


        String message = 'some'


        try {
            withPool(20) {
                def all = whenAllBound([myTask1, myTask2, myTask3], { a, b, c ->
                    println "> $a"
                    println "> $b"
                    println "> $c"
                })
            }
        } catch (Exception ex) {
            println ex.getClass()
            throw ex
        }

        render message
    }

    def notFound() {
        render(status: 404)
    }

    def userTest1() {
        Map user1 = userService.getUser().get()
        Map user2 = userService.getUser().get()
        render([userId: user1.id] as JSON)
    }

    def userTest2() {
        Promise<Map> user = userService.getUser()
        Map user1 = user.get()
        Map user2 = user.get()
        render([userId: user1.id] as JSON)
    }

    private JsonObject getJSON(InputStream inputStream) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        return gson.fromJson(reader, JsonElement.class).getAsJsonObject()
    }
}
