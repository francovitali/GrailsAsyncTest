package com.asyncTest

import org.apache.http.HttpHost
import org.apache.http.conn.routing.HttpRoute
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient

import org.apache.http.impl.nio.reactor.IOReactorConfig
import org.apache.http.nio.reactor.ConnectingIOReactor
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor

//import org.apache.http.conn.HttpClientConnectionManager
//import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.nio.conn.NHttpClientConnectionManager
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager

import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder

/**
 * Created by fvitali on 5/11/16.
 */
class HttpAsyncClientFactory implements InitializingBean, FactoryBean<CloseableHttpAsyncClient> {

    private CloseableHttpAsyncClient _closeableHttpAsyncClient

    int poolSize
    int poolTimeout
    int connectTimeout
    int socketTimeout

    @Override
    CloseableHttpAsyncClient getObject() throws Exception {
        return _closeableHttpAsyncClient
    }

    @Override
    Class<?> getObjectType() {
        return CloseableHttpAsyncClient.class
    }

    @Override
    boolean isSingleton() {
        return true
    }

    @Override
    void afterPropertiesSet() throws Exception {
        println "*** Initializing HttpAsyncClientFactory"

        poolSize = poolSize ?: 2
        poolTimeout = poolTimeout ?: 30
        connectTimeout = connectTimeout ?: 200
        socketTimeout = socketTimeout ?: 2000

        IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
            .setIoThreadCount(poolSize)
            .setConnectTimeout(connectTimeout)
            .setSoTimeout(socketTimeout)
            .build();

        ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
        NHttpClientConnectionManager cnnManager = new PoolingNHttpClientConnectionManager(ioReactor);
        cnnManager.setMaxTotal(50)
        cnnManager.setDefaultMaxPerRoute(10)

        HttpHost localhost = new HttpHost("localhost", 8080)
        HttpRoute someRoute = new HttpRoute(localhost)
        cnnManager.setMaxPerRoute(someRoute, poolSize)

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectionRequestTimeout(poolTimeout)
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(socketTimeout)
            .setStaleConnectionCheckEnabled(true)
            .build()

        _closeableHttpAsyncClient = HttpAsyncClientBuilder
            .create()
            .setConnectionManager(cnnManager)
            .setDefaultRequestConfig(requestConfig)
            .build()

        _closeableHttpAsyncClient.start()
    }
}
