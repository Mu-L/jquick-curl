/*
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
 * Copyright (c) [2025-2099] Martin (goudingcheng@gmail.com)
 */
package com.github.paohaijiao.factory;

import com.github.paohaijiao.support.JCurlCommandProcessor;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

/**
 * packageName com.github.paohaijiao.factory
 *
 * @author Martin
 * @version 1.0.0
 * @className JCurlCommandProcessorFactory
 * @date 2025/6/30
 * @description commons-pool2 object factory for JCurlCommandProcessor
 */
public class JCurlCommandProcessorFactory extends BasePooledObjectFactory<JCurlCommandProcessor> {

    @Override
    public JCurlCommandProcessor create() throws Exception {
        return new JCurlCommandProcessor();
    }

    @Override
    public PooledObject<JCurlCommandProcessor> wrap(JCurlCommandProcessor processor) {
        return new DefaultPooledObject<>(processor);
    }

    @Override
    public boolean validateObject(PooledObject<JCurlCommandProcessor> p) {
        return p.getObject() != null;
    }
}
