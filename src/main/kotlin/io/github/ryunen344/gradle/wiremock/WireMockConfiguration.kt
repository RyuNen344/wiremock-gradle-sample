/*
 * Copyright (C) 2025 RyuNen344
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE.md
 */

package io.github.ryunen344.gradle.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.common.FileSource
import com.github.tomakehurst.wiremock.common.SingleRootFileSource
import com.github.tomakehurst.wiremock.common.Slf4jNotifier
import com.github.tomakehurst.wiremock.core.WireMockApp
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.jetty12.Jetty12HttpServerFactory
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.standalone.MappingsLoader
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.stubbing.StubMappings
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.devtools.restart.RestartScopeInitializer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

@Configuration
class WireMockConfiguration {

    private val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    @Bean
    fun restartScopeInitializer(): RestartScopeInitializer {
        return RestartScopeInitializer()
    }

    @Bean
    fun wireMockServer(
        @Value("\${wiremock.port}") port: Int,
        @Value("\${wiremock.verbose}") verbose: Boolean,
        @Value("\${wiremock.dir}") dir: String,
        @Value("\${wiremock.async}") async: Boolean,
        @Value("\${wiremock.record}") record: Boolean,
        @Value("\${wiremock.proxy}") proxy: String?,
        resourceLoader: ResourceLoader,
    ): WireMockServer {
        logger.info("Configure WireMock server on port $port with verbose=$verbose, dir=$dir, async=$async")
        val fileSource = SingleRootFileSource(resourceLoader.getResource(dir).file).also(FileSource::createIfNecessary)
        val options =
            WireMockConfiguration.options()
                .notifier(Slf4jNotifier(verbose))
                .port(port)
                .httpServerFactory(Jetty12HttpServerFactory())
                .fileSource(fileSource)
                .asynchronousResponseEnabled(async)
        return WireMockServer(options).also {
            if (record) {
                it.enableRecordMappings(
                    fileSource.child(WireMockApp.MAPPINGS_ROOT).also(FileSource::createIfNecessary),
                    fileSource.child(WireMockApp.FILES_ROOT).also(FileSource::createIfNecessary),
                )
            }

            if (proxy != null) {
                it.addProxyMapping(proxy)
            }
        }
    }

    private fun WireMockServer.addProxyMapping(baseUrl: String?) {
        loadMappingsUsing(
            MappingsLoader { stubMappings: StubMappings? ->
                val requestPattern = RequestPatternBuilder.newRequestPattern(RequestMethod.ANY, WireMock.anyUrl()).build()
                val responseDef = ResponseDefinitionBuilder.responseDefinition().proxiedFrom(baseUrl).build()

                val proxyBasedMapping = StubMapping(requestPattern, responseDef)
                proxyBasedMapping.priority = 10
                stubMappings!!.addMapping(proxyBasedMapping)
            },
        )
    }
}
