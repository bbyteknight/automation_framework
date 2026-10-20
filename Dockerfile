# syntax=docker/dockerfile:1
FROM maven:3.9-eclipse-temurin-21

# Headless Chrome for @web Selenium scenarios. WebDriverManager (already a
# project dependency) downloads a matching chromedriver at test runtime —
# only the browser binary itself needs to exist in the image.
RUN apt-get update && apt-get install -y --no-install-recommends \
        wget ca-certificates fonts-liberation \
    && wget -q -O /tmp/google-chrome.deb https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb \
    && apt-get install -y --no-install-recommends /tmp/google-chrome.deb \
    && rm -f /tmp/google-chrome.deb \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Cache dependencies as their own image layer: only re-downloaded when pom.xml
# changes, not on every source-code edit. Subsequent `docker build`s reuse
# this layer straight from the local image cache instead of hitting Maven
# Central again.
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY . .

# Reports/screenshots are meant to be bind-mounted from the host so results
# survive after the container exits — see README.md "Running in Docker".
VOLUME ["/app/target", "/app/test-output"]

ENV HEADLESS=true

# `docker run <image> test -Psmoke -Dbrowser=firefox ...` appends straight
# onto `mvn -B -ntp`, so every CLI flag documented in README.md works
# unchanged inside the container.
ENTRYPOINT ["mvn", "-B", "-ntp"]
CMD ["test"]
