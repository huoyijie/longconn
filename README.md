# longconn
Netty based http proxy server, support push, and long connection based async http client(All for learn), 
Suitable for App unified HTTP API gateway, security, high performance and support for high concurrency.

# modules
1.http proxy server(push proxy)
2.async http client(thread pool)
3.codec encoder/decoder(messagepack)
4.command protocal(httprequest, httpresponse, pushrequest, heartbeat)

# features
1.long connection based netty
2.support GET/POST http(s)
3.support push
4.ssl/gzip
5.async http client, support future/callback
6.messagepack serialization

# reference
1.[Netty](https://github.com/netty/netty)

2.[async-http-client](https://github.com/AsyncHttpClient/async-http-client)

3.[messagepack](https://github.com/msgpack/msgpack-java)

