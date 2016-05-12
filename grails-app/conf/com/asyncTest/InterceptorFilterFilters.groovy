package com.asyncTest

import grails.converters.JSON
import org.apache.http.params.HttpConnectionParams

import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import groovyx.net.http.AsyncHTTPBuilder

import java.util.concurrent.TimeUnit
import static groovyx.gpars.dataflow.Dataflow.task

class InterceptorFilterFilters {
    private final String REQUEST_KEY = "__someKey"

    def filters = {
        all(controller: '*', action: '*') {
            before = {
                return
                println '****** BEFORE *****'
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


                Future<String> myTask = asyncHttpBuilder.get(path: '/AsyncTest/Test/slowAction', query: [seconds: 10]) { resp, json ->
                    json.message
                }
                myTask.

                request.setAttribute(REQUEST_KEY, myTask)
            }
            after = { Map model ->
                return
                println '****** AFTER *****'
                try {
                    Future<String> myTask = request.getAttribute(REQUEST_KEY)
                    myTask.get(600L, TimeUnit.MILLISECONDS)
                } catch (java.util.concurrent.TimeoutException ex) {
                    log.error("*** Internal Error ***")
                    controller
                    boolean userRequired = controllerAction.isAnnotationPresent(UserRequired) && controllerAction.getAnnotation(UserRequired).value()

                    if (userRequired) {
                        log.error("User is required!", ex)
                        throw new Exception("The user is required!")
                        //render([exception: ex.getCause().getClass(), message: ex.getCause().getMessage()] as JSON)
                        //return
                    }
                }
            }
            afterView = { Exception e ->

            }
        }
    }
}
