/**
 * blackduck-artifactory-common
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.blackduck.artifactory;

public class PluginConstants {
    public static final String PUBLIC_DOCUMENTATION_LINK = "https://synopsys.atlassian.net/wiki/spaces/INTDOCS/pages/32178187/Black+Duck+Artifactory+Plugin";
    public static final boolean DISABLE_OLD_FUNCTIONALITY = false;
    public static final boolean ENABLE_NEW_FUNCTIONALITY = !DISABLE_OLD_FUNCTIONALITY; // TODO: Remove DISABLE_OLD_FUNCTIONALITY & ENABLE_NEW_FUNCTIONALITY upon reaching feature parity
}
