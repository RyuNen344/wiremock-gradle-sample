WireMockGradleSample
====

A Sample Project for WireMock with Gradle

### How to Use

1. place your OpenAPI spec
2. update `build.gradle` with your OpenAPI spec path
3. run `./gradlew openApiGenerates`, this will generate the WireMock stubs
4. modify [`WireMockApplicationRunner`](src/main/kotlin/io/github/ryunen344/gradle/wiremock/WireMockApplicationRunner.kt) to use your generated stubs
5. run the application

### How to Run

#### Gradle

```shell
./gradlew classes -t & ./gradlew bootRun
```

#### IntelliJ IDEA

`Preferences > Build,Execution,Deployment > Compiler > Build Project automatically`

### Options

- you can change server config by modifying [`application.yaml`](src/main/resources/application.yaml)
- supported options:
    - `wiremock.verbose`: whether to enable verbose logging (default: true)
    - `wiremock.port`: the port WireMock server will run on (default: 8080)
    - `wiremock.dir`: the directory where WireMock stubs are stored (default: "wiremock")
    - `wiremock.async`: whether to run WireMock in asynchronous mode (default: true)
    - `wiremock.record`: whether to record requests (default: true)
    - `wiremock.proxy`: the URL of the proxy server (default: null)
- see also [`WireMockConfiguration`](src/main/kotlin/io/github/ryunen344/gradle/wiremock/WireMockConfiguration.kt)

## License
```text
Copyright (C) 2025 RyuNen344

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
```
