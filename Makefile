.PHONY: build
build:
	lein compile

.PHONY: install
install:
	lein install

.PHONY: clean
clean:
	lein clean

.PHONY: package
package:
	lein clean
	lein release patch
