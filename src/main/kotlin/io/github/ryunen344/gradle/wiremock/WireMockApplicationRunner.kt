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
import com.github.tomakehurst.wiremock.common.Json
import com.github.tomakehurst.wiremock.matching.AnythingPattern
import org.openapi.example.api.PetApiStubs
import org.openapi.example.api.StoreApiMockServer
import org.openapi.example.model.Order
import org.openapi.example.model.Pet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import kotlin.system.exitProcess

@Component
class WireMockApplicationRunner(private val server: WireMockServer) : ApplicationRunner {

    private val logger: Logger by lazy { LoggerFactory.getLogger(this::class.java) }

    override fun run(args: ApplicationArguments) {
        try {
            val loadedExtensionNames = server.loadedExtensionNames.orEmpty()
            if (loadedExtensionNames.isNotEmpty()) {
                logger.info("Loaded extensions: {}", loadedExtensionNames.joinToString(", "))
            }

            logger.info("Load Stubs from OAS generated code")
            val objectMapper = Json.getObjectMapper()

            // region: PetApi
            val petApiStubs = PetApiStubs(objectMapper)
            server.stubFor(petApiStubs.addPet().respondWith405())
            server.stubFor(
                petApiStubs
                    .getPetById(AnythingPattern())
                    .respondWith200(
                        Pet(
                            name = "Eve Cox",
                            photoUrls = listOf("https://example.com/photo1.jpg", "https://example.com/photo2.jpg"),
                            id = 213334,
                            category = null,
                            tags = listOf(),
                            status = "interpretaris",
                        ),
                    ),
            )
            // endregion

            // region: StoreApi
            server.stubFor(
                StoreApiMockServer.stubGetInventory200(
                    objectMapper.writeValueAsString(mapOf("available" to 0, "pending" to 1, "sold" to 1)),
                ),
            )
            server.stubFor(
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
            exitProcess(1)
        }
    }
}
