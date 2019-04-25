/**
 * blackduck-artifactory-common
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.blackduck.artifactory.modules.inspection.notifications;

import java.util.List;

import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.OriginView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;

public class CompositeComponentModel {
    private final VersionBomComponentView versionBomComponentView;
    private final ComponentVersionView componentVersionView;
    private final List<OriginView> originViews;

    public CompositeComponentModel(final VersionBomComponentView versionBomComponentView, final ComponentVersionView componentVersionView, final List<OriginView> originViews) {
        this.versionBomComponentView = versionBomComponentView;
        this.componentVersionView = componentVersionView;
        this.originViews = originViews;
    }

    public VersionBomComponentView getVersionBomComponentView() {
        return versionBomComponentView;
    }

    public ComponentVersionView getComponentVersionView() {
        return componentVersionView;
    }

    public List<OriginView> getOriginViews() {
        return originViews;
    }
}
