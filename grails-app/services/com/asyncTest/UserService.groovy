package com.asyncTest

import groovyx.gpars.dataflow.Promise
import groovyx.net.http.AsyncHTTPBuilder
import org.apache.http.client.params.ClientPNames
import org.apache.http.conn.params.ConnManagerParams
import org.apache.http.params.HttpConnectionParams

import java.util.concurrent.Future

import static groovyx.gpars.dataflow.Dataflow.task
import static java.lang.Thread.*

/**
 * Created by fvitali on 5/10/16.
 */
class UserService {

    AsyncHTTPBuilder asyncHttpBuilder

    UserService() {
        Map<String, Object> params = new HashMap<String, Object>()
        int poolSize = 1
        String baseUri = 'http://localhost:8080'
        long poolConnectionTimeout = 500
        int connectionTimeout = 100
        int socketTimeout = 20000

        params.put("poolSize", poolSize);
        params.put("uri", baseUri);
        params.put("contentType", groovyx.net.http.ContentType.JSON)

        asyncHttpBuilder = new AsyncHTTPBuilder(params)

        def httpParams = asyncHttpBuilder.getClient().getParams()

        HttpConnectionParams.setConnectionTimeout(httpParams, connectionTimeout)
        HttpConnectionParams.setSoTimeout(httpParams, socketTimeout)

        //httpParams.setParameter(ClientPNames.CONN_MANAGER_TIMEOUT, poolConnectionTimeout)
        httpParams.setLongParameter("http.conn-manager.timeout", poolConnectionTimeout);

        //ConnManagerParams.setTimeout(httpParams, poolConnectionTimeout)
    }

    Promise<Map> getUser() {
        println '*** UserService.getUser'
        task {
            println '*** UserService.getUser.task'
            [name: 'Franco', id: task {
                println '*** UserService.getUser.task.id'
                666
            }.get()]
        }
    }

    def getSome(int seconds) {
        println "Executing getSome ($seconds)"
        Future<String> myPromise1 = asyncHttpBuilder.get(path: '/AsyncTest/Test/slowAction', query: [seconds: seconds]) { resp, json ->
            json.message
        }
        myPromise1.get()
    }
}
