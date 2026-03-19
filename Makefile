.PHONY: build run test verify package clean

build:
	./gradlew build

run:
	./gradlew runIde

test:
	./gradlew build verifyPlugin

verify:
	./gradlew verifyPlugin

package:
	./gradlew buildPlugin

clean:
	./gradlew clean
