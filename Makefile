.PHONY: help
 help:	                                       ## Show list of available make targets
	@cat Makefile | grep -e "^[a-zA-Z_\-]*: *.*## *" | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

.PHONY: build
build:                                         ## Compile the project
	lein compile

.PHONY: test
test: build                                    ## Run the unit test suite and check formatting of the source code
	lein cljfmt check
	lein test

.PHONY: format
format:                                        ## Reformat Clojure source code
	lein cljfmt fix

.PHONY: install
install:                                       ## Install a snapshot release locally
	lein install

.PHONY: clean
clean:                                         ## Clean the local build directory
	lein clean

.PHONY: package
package: test                                  ## Package a new release of the application
	lein clean
	lein release patch
