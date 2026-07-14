.PHONY: build run dev test verify package clean

build:
	./gradlew build

run:
	./gradlew runIde

# Build and open a sandbox IDE with the plugin installed (the IntelliJ analog
# of VS Code's Extension Development Host). Blocks until the sandbox is closed.
dev:
	./gradlew runIde

test:
	./gradlew build verifyPlugin

verify:
	./gradlew verifyPlugin

package:
	./gradlew buildPlugin

clean:
	./gradlew clean
