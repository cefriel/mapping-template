/*
 * Copyright (c) 2019-2024 Cefriel.
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
 */

package com.cefriel.template.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.cefriel.template.utils.TemplateFunctions.encodeURIComponent;

public class URLComponents {
    private String scheme;
    private String host;
    private int port;
    private String path;
    private String fragment;

    public URLComponents(String url) {
        // Scheme
        int schemeEnd = url.indexOf("://");
        if (schemeEnd != -1) {
            this.scheme = url.substring(0, schemeEnd);
            url = url.substring(schemeEnd + 3); // Remove scheme and '://'
        }

        // Fragment
        int fragmentStart = url.indexOf("#");
        if (fragmentStart != -1) {
            this.fragment = url.substring(fragmentStart + 1);
            url = url.substring(0, fragmentStart); // Remove fragment
        }

        // Host and Port
        int pathStart = url.indexOf("/");
        String hostPort = (pathStart != -1) ? url.substring(0, pathStart) : url;
        if (pathStart != -1) {
            this.path = url.substring(pathStart); // Path
        }

        int portStart = hostPort.indexOf(":");
        if (portStart != -1) {
            this.host = hostPort.substring(0, portStart);
            this.port = Integer.parseInt(hostPort.substring(portStart + 1));
        } else {
            this.host = hostPort;
            this.port = -1; // Port not found
        }
    }


    public String getEncodedURL() {
        // Encode the path segments
        String encodedPath = Arrays.stream(path.split("/"))
                .map(TemplateFunctions::encodeURIComponent)
                .collect(Collectors.joining("/"));

        // TODO Check why ; should not be encoded in 005a
        encodedPath = encodedPath.replaceAll("%3B", ";");

        // Encode the fragment
        String encodedFragment = fragment != null ? encodeURIComponent(fragment) : "";

        // Reassemble the encoded URL
        return scheme + "://" + host +
                (port != -1 ? ":" + port : "") +
                encodedPath +
                (encodedFragment.isEmpty() ? "" : "#" + encodedFragment);
    }
}

