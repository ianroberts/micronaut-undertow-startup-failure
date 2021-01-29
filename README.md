# Micronaut undertow startup failure - demo app

This is a very simple app to demonstrate an issue whereby a Micronaut app using the `micronaut-http-server-undertow` web server will fail to start in the following situation:

- `micronaut.server.host` set to `0.0.0.0` (i.e. bind to all addresses, not just localhost)
- host has a dual-stack IPv4/v6 network

## Steps to reproduce

```
./gradlew shadowJar
java -jar build/libs/micronaut-undertow-startup-failure-0.1-all.jar
```

App startup fails with the following exception:

```
17:36:12.257 [main] ERROR io.micronaut.runtime.Micronaut - Error starting Micronaut server: For input string: "0:0:0:0:0:0:0:8080"
io.micronaut.http.server.exceptions.InternalServerException: For input string: "0:0:0:0:0:0:0:8080"
	at io.micronaut.servlet.undertow.UndertowServer.getURL(UndertowServer.java:108)
	at io.micronaut.runtime.Micronaut.lambda$start$2(Micronaut.java:95)
	at java.util.Optional.ifPresent(Optional.java:159)
	at io.micronaut.runtime.Micronaut.start(Micronaut.java:75)
	at io.micronaut.runtime.Micronaut.run(Micronaut.java:311)
	at io.micronaut.runtime.Micronaut.run(Micronaut.java:297)
	at com.example.Application.main(Application.java:8)
Caused by: java.net.MalformedURLException: For input string: "0:0:0:0:0:0:0:8080"
	at java.net.URL.<init>(URL.java:627)
	at java.net.URL.<init>(URL.java:490)
	at java.net.URL.<init>(URL.java:439)
	at java.net.URI.toURL(URI.java:1089)
	at io.micronaut.servlet.undertow.UndertowServer.getURL(UndertowServer.java:106)
	... 6 common frames omitted
Caused by: java.lang.NumberFormatException: For input string: "0:0:0:0:0:0:0:8080"
	at java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)
	at java.lang.Integer.parseInt(Integer.java:580)
	at java.lang.Integer.parseInt(Integer.java:615)
	at java.net.URLStreamHandler.parseURL(URLStreamHandler.java:222)
	at java.net.URL.<init>(URL.java:622)
	... 10 common frames omitted
```

## Workarounds

Any of the following will avoid the error:

- Do not set `micronaut.server.host` in application.yml (though then the app binds only to localhost by default)
- Disable IPv6 with `-Djava.net.preferIPv4Stack=true`
- Configure the logger for `io.micronaut.runtime.Micronaut` to be `ERROR` or higher
  - this fixes it because the failing call to `UndertowServer.getURL` is guarded by an `if(LOG.isInfoEnabled())`

## Analysis

The root cause appears to be that in this case `UndertowServer.getHost()` is returning the string form of an IPv6 all-zeros address, which `getURI` blindly concatenates in `URI.create(getScheme() + "://" + getHost() + ":" + getPort())` without wrapping it in square brackets.  Safer would be to use the multi-argument URI constructor, which handles all necessary escaping:

```
new URI(getScheme(), null, getHost(), getPort(), null, null, null)
```
