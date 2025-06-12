/*
 * Copyright (C) 2025-2025 RyuNen344
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
import com.github.tomakehurst.wiremock.common.Json
import com.github.tomakehurst.wiremock.core.WireMockApp
import com.github.tomakehurst.wiremock.http.HttpServerFactory
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.jetty12.Jetty12HttpServerFactory
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.standalone.CommandLineOptions
import com.github.tomakehurst.wiremock.standalone.MappingsLoader
import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.stubbing.StubMappings
import org.openapi.example.api.PetApiStubs
import org.openapi.example.api.StoreApiMockServer
import org.openapi.example.model.Order
import org.openapi.example.model.Pet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlin.system.exitProcess

/** This class is copied from [WireMockServerRunner] to access [WireMockServer] directly */
@SpringBootApplication
object WireMockApplication : ApplicationRunner {

    private val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    private var server: WireMockServer? = null

    @JvmStatic
    fun main(vararg args: String) {
        runApplication<WireMockApplication>(*args)
    }

    override fun run(args: ApplicationArguments) {
        val options =
            object : CommandLineOptions(*args.sourceArgs) {
                override fun httpServerFactory(): HttpServerFactory {
                    return Jetty12HttpServerFactory()
                }
            }
        val fileSource = options.filesRoot()
        fileSource.createIfNecessary()
        val filesFileSource = fileSource.child(WireMockApp.FILES_ROOT)
        filesFileSource.createIfNecessary()
        val mappingsFileSource = fileSource.child(WireMockApp.MAPPINGS_ROOT)
        mappingsFileSource.createIfNecessary()

        server?.stop()
        server = WireMockServer(options)

        if (options.recordMappingsEnabled()) {
            server?.enableRecordMappings(mappingsFileSource, filesFileSource)
        }

        if (options.specifiesProxyUrl()) {
            server?.addProxyMapping(options.proxyUrl())
        }

        try {
            server?.start()
            val https = options.httpsSettings().enabled()

            if (!options.httpDisabled) {
                server?.port()?.let(options::setActualHttpPort)
            }

            if (https) {
                server?.httpsPort()?.let(options::setActualHttpsPort)
            }

            println(options)

            val loadedExtensionNames = server?.loadedExtensionNames.orEmpty()
            if (loadedExtensionNames.isNotEmpty()) {
                println("Loaded extensions: ${loadedExtensionNames.joinToString(", ")}")
            }

            println("Load Stubs from OAS generated code")
            val objectMapper = Json.getObjectMapper()

            // region: PetApi
            val petApiStubs = PetApiStubs(objectMapper)
            server?.stubFor(petApiStubs.addPet().respondWith405())
            server?.stubFor(
                petApiStubs
                    .getPetById(AnythingPattern())
                    .respondWith200(
                        Pet(
                            name = "Eve Cox",
                            photoUrls = listOf(
                                "https://example.com/photo1.jpg",
                                "https://example.com/photo2.jpg",
                            ),
                            id = 213334,
                            category = null,
                            tags = listOf(),
                            status = "interpretaris",
                        ),
                    ),
            )
            // endregion

            // region: StoreApi
            server?.stubFor(
                StoreApiMockServer.stubGetInventory200(
                    objectMapper.writeValueAsString(mapOf("available" to 0, "pending" to 1, "sold" to 1)),
                ),
            )
            server?.stubFor(
                StoreApiMockServer.stubGetOrderById200(
                    "orderId",
                    objectMapper.writeValueAsString(
                        Order(
                            id = 123456,
                            petId = 67890,
                            quantity = 2,
                            shipDate = java.time.OffsetDateTime.now(),
                            status = "placed",
                            complete = true,
                        ),
                    ),
                ),
            )
            // endregion
        } catch (e: Exception) {
            logger.error("Failed to start WireMock server", e)
            server?.stop()
            server = null
            exitProcess(1)
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
