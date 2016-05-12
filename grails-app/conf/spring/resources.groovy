import com.asyncTest.HttpAsyncClientFactory
import com.asyncTest.HttpClientFactory

// Place your Spring DSL code here
beans = {
    httpClient(HttpClientFactory) {
        poolSize = 2
        poolTimeout = 30
        connectTimeout = 200
        socketTimeout = 3000
    }
    httpAsyncClient(HttpAsyncClientFactory) {
        poolSize = 2
        poolTimeout = 30
        connectTimeout = 200
        socketTimeout = 3000
    }
}
