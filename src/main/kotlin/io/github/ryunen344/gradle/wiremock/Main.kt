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

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.Version
import com.github.tomakehurst.wiremock.core.WireMockApp
import com.github.tomakehurst.wiremock.http.RequestMethod
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder
import com.github.tomakehurst.wiremock.standalone.CommandLineOptions
import com.github.tomakehurst.wiremock.standalone.MappingsLoader
import com.github.tomakehurst.wiremock.standalone.WireMockServerRunner
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.github.tomakehurst.wiremock.stubbing.StubMappings
import org.openapi.example.api.PetApiStubs
import org.openapi.example.model.Pet
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

/** This class is copied from [WireMockServerRunner] to access [WireMockServer] directly */
object Main {

    private const val BANNER: String =
        ("\n" +
            "\u001B[34m██     ██ ██ ██████  ███████ \u001B[33m███    ███  ██████   ██████ ██   ██ \n" +
            "\u001B[34m██     ██ ██ ██   ██ ██      \u001B[33m████  ████ ██    ██ ██      ██  ██  \n" +
            "\u001B[34m██  █  ██ ██ ██████  █████   \u001B[33m██ ████ ██ ██    ██ ██      █████   \n" +
            "\u001B[34m██ ███ ██ ██ ██   ██ ██      \u001B[33m██  ██  ██ ██    ██ ██      ██  ██  \n" +
            "\u001B[34m ███ ███  ██ ██   ██ ███████ \u001B[33m██      ██  ██████   ██████ ██   ██ \n" +
            "\n\u001B[0m" +
            "----------------------------------------------------------------\n" +
            "|               Cloud: https://wiremock.io/cloud               |\n" +
            "|                                                              |\n" +
            "|               Slack: https://slack.wiremock.org              |\n" +
            "----------------------------------------------------------------")

    private val logger = LoggerFactory.getLogger(this::class.java)

    private var server: WireMockServer? = null

    @JvmStatic
    fun main(vararg args: String) {

        try {
            val options = CommandLineOptions(*args)
            if (options.help()) {
                println(options.helpText())
                return
            }
            if (options.version()) {
                println(Version.getCurrentVersion())
                return
            }

            val fileSource = options.filesRoot()
            fileSource.createIfNecessary()
            val filesFileSource = fileSource.child(WireMockApp.FILES_ROOT)
            filesFileSource.createIfNecessary()
            val mappingsFileSource = fileSource.child(WireMockApp.MAPPINGS_ROOT)
            mappingsFileSource.createIfNecessary()

            server = WireMockServer(options)

            if (options.recordMappingsEnabled()) {
                server?.enableRecordMappings(mappingsFileSource, filesFileSource)
            }

            if (options.specifiesProxyUrl()) {
                addProxyMapping(options.proxyUrl())
            }

            server?.start()
            val https = options.httpsSettings().enabled()

            if (!options.httpDisabled) {
                server?.port()?.let(options::setActualHttpPort)
            }

            if (https) {
                server?.httpsPort()?.let(options::setActualHttpsPort)
            }

            if (!options.bannerDisabled()) {
                println(BANNER)
                println()
            } else {
                println()
                println("The WireMock server is started .....")
            }
            println(options)

            val loadedExtensionNames = server?.loadedExtensionNames.orEmpty()
            if (loadedExtensionNames.isNotEmpty()) {
                println("Loaded extensions: ${loadedExtensionNames.joinToString(", ")}")
            }

            println("Load Stubs from OAS generated code")

            server?.let {
                val mapper = ObjectMapper()
                val petApiStubs = PetApiStubs(mapper)
                it.stubFor(petApiStubs.addPet().respondWith(200, "Pet added successfully"))
                it.stubFor(
                    petApiStubs
                        .getPetById(AnythingPattern())
                        .respondWith200(
                            Pet(
                                name = "Eve Cox",
                                photoUrls = listOf(),
                                id = 2134,
                                category = null,
                                tags = listOf(),
                                status = "interpretaris",
                            ),
                        ),
                )
            }
        } catch (e: Exception) {
            logger.error("Failed to start WireMock server", e)
            server?.stop()
            server = null
            exitProcess(1)
        }
    }

    private fun addProxyMapping(baseUrl: String?) {
        server?.loadMappingsUsing(
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
