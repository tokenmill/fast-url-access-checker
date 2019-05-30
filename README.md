# URL Access Checker

This tool will take a list URLs of the sites, identify a correct form of the URL and run HTTP GET request against the URL to check its HTTP status. In cases where the address is not completely specified - a protocol is missing, 'www' part is not included when it is needed - a correct form of the URL will be identified. The library will also validate the correctness of the URL and in cases of redirection will return a target URL.

It is a Clojure library. Interface to call it natively from Java is provided. A native binary distribution to be used as a command line tool will be provided in the next release.

# Features

* Provides the interface for a single URL check.
* Provides the interface for bulk URL checks.
* In the case of bulk URL check library parallelizes checking to ensure maximum speed of the entire process.
* In cases of incompletely formed URLs correct protocol (http or https) will be detected. Access with 'www' part if it is missing will also be tested.
* Redirection will be detected and target URL returned.
* URL check returns the following data: HTTP status, target URL, response time.

# How to Use

The library can be included as a dependency via maven, lein, deps.edn

## Clojure

Singe URL check example.

```
(require '[fast-url-check.core :refer :all])

(check-access "tokenmill.lt")
=> 
{:url "http://www.tokenmill.lt/",
 :seed "tokenmill.lt",
 :status 200,
 :response-time 7,
 :status-type :accessible}
```

Bulk URL check example

```

(check-access-bulk ["tokenmill.lt" "15min.lt" "https://news.ycombinator.com"])
=> 
({:url "http://www.tokenmill.lt/",
  :seed "tokenmill.lt",
  :status 200,
  :response-time 10,
  :status-type :accessible}
 {:url "https://www.15min.lt/",
  :seed "15min.lt",
  :status 200,
  :response-time 46,
  :status-type :accessible}
 {:url "https://news.ycombinator.com/",
  :seed "https://news.ycombinator.com",
  :status 301,
  :response-time 379,
  :status-type :redirect})

```

## Java

Java code example:

```
import crawl.tools.URLCheck;

import java.util.Map;
import java.util.Arrays;
import java.util.Collection;

public class MyClass {

    public static void main(String[] args) {
        System.out.println(URLCheck.checkAccess("tokenmill.lt"));

        String[] urls = {"15min.lt", "https://news.ycombinator.com"};
        Collection<Map> validatedUrls = URLCheck.checkAccessBulk(Arrays.asList(urls));
        for(Map validatedUrl : validatedUrls) {
            System.out.println(validatedUrl);
        }
    }
}

```

# Benchmark

This tool aims to provide top performance in bulk URL checking. This repository includes a [reference set](https://github.com/tokenmill/fast-url-access-checker/blob/master/test/resources/bulk-test.txt) of 1000 URLs for consistent performance checking. 

Benchmark test executed against the reference URL set performs with average _0.3 seconds per URL_. Execution times are subject to the network conditions and hardware the tests are executed on.

Benchmark test can be launched with `make benchmark`

# Licensing

Distributed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html)
