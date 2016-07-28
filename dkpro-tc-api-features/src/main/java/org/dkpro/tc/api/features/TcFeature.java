/*******************************************************************************
 * Copyright 2016
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.dkpro.tc.api.features;

import java.util.Map;

import org.apache.uima.resource.ExternalResourceDescription;
import org.dkpro.lab.task.Discriminable;

public abstract class TcFeature implements Discriminable
{
    private String name;
    protected Map<String, Object> config;
    private String fullFeatureName;
    
    public TcFeature(String aName, String fullFeatureName)
    {
        name = aName;
        this.fullFeatureName = fullFeatureName;
    }

    public void setConfig(Map<String, Object> aConfig) {
        config = aConfig;
    }
    
    @Override
    public Object getDiscriminatorValue()
    {
        return name;
    }
    
    @Override
    public abstract ExternalResourceDescription getActualValue();

    public String getFeatureName()
    {
        return fullFeatureName;
    }
}