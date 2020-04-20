/*
 * Copyright (C) 2020 Bosch.IO GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

package org.ossreviewtoolkit.utils

import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.SocketAddress
import java.net.URI

fun String.toProxy(): Proxy? {
    val uri = runCatching {
        // Assume http if no protocol is specified to be able to create an URI.
        URI(takeIf { it.contains("://") } ?: "http://$this")
    }.getOrElse {
        return null
    }

    val type = when {
        uri.scheme.startsWith("http") -> Proxy.Type.HTTP
        uri.scheme.startsWith("socks") -> Proxy.Type.SOCKS
        else -> throw IllegalArgumentException("Unsupported proxy scheme.")
    }

    val port = uri.port.takeIf { it in IntRange(0, 65535) } ?: 8080
    return Proxy(type, InetSocketAddress(uri.host, port))
}

private val NO_PROXY_LIST = listOf(Proxy.NO_PROXY)

typealias ProxyMap = Map<String, List<Proxy>>

class OrtProxySelector : ProxySelector() {
    private val proxyOrigins = mutableMapOf<String, ProxyMap>().also {
        addProxyOrigin("env", mapOf(
            "http" to listOfNotNull(Os.env["http_proxy"]?.toProxy())
        ))

        addProxyOrigin("env", mapOf(
            "https" to listOfNotNull(Os.env["https_proxy"]?.toProxy())
        ))
    }

    fun addProxyOrigin(origin: String, map: ProxyMap) {
        proxyOrigins[origin] = map
    }

    fun removeProxyOrigin(origin: String) = proxyOrigins.remove(origin)

    override fun select(uri: URI?): List<Proxy> {
        val proxies = proxyOrigins.flatMap { (_, proxyMap) -> proxyMap.getOrDefault(uri?.scheme, emptyList()) }

        // Quote from the upstream documentation for select: When no proxy is available, the list will contain one
        // element of type Proxy that represents a direct connection.
        return proxies.takeUnless { it.isEmpty() } ?: NO_PROXY_LIST
    }

    override fun connectFailed(uri: URI?, sa: SocketAddress?, ioe: IOException?) {}
}
