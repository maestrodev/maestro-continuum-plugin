/*
 * Copyright 2012, MaestroDev
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

package com.maestrodev.maestrocontinuumplugin;

import org.apache.maven.continuum.xmlrpc.client.ContinuumXmlRpcClient;

import java.net.URL;

public class ContinuumXmlRpcClientFactory {

    private static ContinuumXmlRpcClientFactory theInstance;

    public static ContinuumXmlRpcClientFactory getInstance() {

        if (theInstance == null) {
            synchronized (ContinuumXmlRpcClientFactory.class) {
                if (theInstance == null)
                    theInstance = new ContinuumXmlRpcClientFactory();
            }
        }
        return theInstance;
    }


    private ContinuumXmlRpcClientFactory() {

    }

    public ContinuumXmlRpcClient getClient(URL url, String username, String password) {
        return new ContinuumXmlRpcClient(url, username, password);
    }


}
