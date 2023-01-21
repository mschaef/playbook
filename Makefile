.PHONY: build
build:
	lein compile

.PHONY: install
install:
	lein install

.PHONY: clean
clean:
	lein clean

.PHONY: deploy
deploy:
	lein clean
	lein release patch
