package com.asyncTest

import org.apache.http.HttpHost
import org.apache.http.conn.routing.HttpRoute
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.apache.http.impl.client.CloseableHttpClient

import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.HttpClientBuilder

/**
 * Created by fvitali on 5/11/16.
 */
class HttpClientFactory implements InitializingBean, FactoryBean<CloseableHttpClient> {

    private CloseableHttpClient _closeableHttpClient

    int poolSize
    int poolTimeout
    int connectTimeout
    int socketTimeout

    @Override
    CloseableHttpClient getObject() throws Exception {
        return _closeableHttpClient
    }

    @Override
    Class<?> getObjectType() {
        return CloseableHttpClient.class
    }

    @Override
    boolean isSingleton() {
        return true
    }

    @Override
    void afterPropertiesSet() throws Exception {
        println "*** Initializing HttpClientFactory"

        poolSize = poolSize ?: 2
        poolTimeout = poolTimeout ?: 30
        connectTimeout = connectTimeout ?: 200
        socketTimeout = socketTimeout ?: 2000

        HttpClientConnectionManager cnnManager = new PoolingHttpClientConnectionManager()
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

        _closeableHttpClient = HttpClientBuilder
            .create()
            .setConnectionManager(cnnManager)
            .setDefaultRequestConfig(requestConfig)
            .build()
    }
}

