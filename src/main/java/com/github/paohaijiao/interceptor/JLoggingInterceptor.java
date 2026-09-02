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
package com.github.paohaijiao.interceptor;

import com.github.paohaijiao.console.JConsole;
import com.github.paohaijiao.enums.JCurlLevelLog;
import com.github.paohaijiao.enums.JLogLevel;
import com.github.paohaijiao.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * packageName com.paohaijiao.javelin.interceptor
 *
 * @author Martin
 * @version 1.0.0
 * @className JLoggingInterceptor
 * @date 2025/6/21
 * @description
 */
public class JLoggingInterceptor implements Interceptor {

    private static final Charset UTF8 = StandardCharsets.UTF_8;

    private JConsole console=new JConsole();

    private final JCurlLevelLog level;

    public JLoggingInterceptor() {
        this(JCurlLevelLog.ALL);
    }

    public JLoggingInterceptor(JCurlLevelLog level) {
        this.level = level;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        long startNs = System.nanoTime();
        Response response;
        try {
            response = chain.proceed(request);
        } catch (Exception e) {
            console.error("<-- HTTP FAILED: " , e);
            throw e;
        }
        long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);
        console.log(JLogLevel.INFO," the request cost : "+tookMs+" ms,");
        return response;
    }
}
