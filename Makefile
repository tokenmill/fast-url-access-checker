unit-test:
	clojure -A:test -e integration

integration-test:
	clojure -A:test -i integration

lint:
	clojure -A:kibit
	clojure -A:eastwood

uberjar:
	clojure -A:uberjar

check-urls:
	clojure -m fast-url-check.core $(file-name)

benchmark:
	clojure -A:dev -m fast-url-check.benchmark $(file-name)
