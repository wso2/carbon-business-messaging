/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.service.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A container class for destinations. This will also have other properties based on offsets and limits.
 */
@ApiModel(value = "Destinations Container", description = "A container class for destinations.")
public class DestinationsContainer {
    @ApiModelProperty(value = "Total number of destinations.", required = true)
    private int totalDestinations = 0;
    @ApiModelProperty(value = "Url for the next set of destinations.")
    private String next = StringUtils.EMPTY;
    @ApiModelProperty(value = "Url for the previous set of destinations.")
    private String previous = StringUtils.EMPTY;
    @ApiModelProperty(value = "The list of destinations.", required = true)
    private List<Destination> destinations = new ArrayList<>();

    public int getTotalDestinations() {
        return totalDestinations;
    }

    public String getNext() {
        return next;
    }

    public String getPrevious() {
        return previous;
    }

    public List<Destination> getDestinations() {
        return destinations;
    }

    public void setTotalDestinations(int totalDestinations) {
        this.totalDestinations = totalDestinations;
    }

    public void setNext(String next) {
        this.next = next;
    }

    public void setPrevious(String previous) {
        this.previous = previous;
    }

    public void setDestinations(List<Destination> destinations) {
        this.destinations = destinations;
    }
}
