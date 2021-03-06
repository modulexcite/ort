/*
 * Copyright (C) 2017-2019 HERE Europe B.V.
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

/**
 * Operating-System-specific utility functions.
 */
object Os {
    val name = System.getProperty("os.name").orEmpty()
    private val nameLowerCase = name.toLowerCase()

    val isLinux = "linux" in nameLowerCase
    val isMac = "mac" in nameLowerCase
    val isWindows = "windows" in nameLowerCase

    val env = System.getenv().let { env ->
        if (isWindows) env.toSortedMap(String.CASE_INSENSITIVE_ORDER) else env.toSortedMap()
    }

    val proxy = listOf(env["https_proxy"], env["http_proxy"]).find {
        it != null
    }?.let { proxy ->
        // Note that even HTTPS proxies use "http://" as the protocol!
        proxy.takeIf { it.startsWith("http") } ?: "http://$proxy"
    }
}
