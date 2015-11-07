/*
 * Copyright 2015 Netflix, Inc.
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
 */

package io.reactivex.netty.spectator.http;

import com.netflix.spectator.api.Counter;

import static io.reactivex.netty.spectator.SpectatorUtils.*;

public class ResponseCodesHolder {

    private final Counter response5xx;
    private final Counter response4xx;
    private final Counter response3xx;
    private final Counter response2xx;
    private final Counter response1xx;

    public ResponseCodesHolder(String monitorId) {
        response1xx = newCounter("responseCodes", monitorId, "code", "1xx");
        response2xx = newCounter("responseCodes", monitorId, "code", "2xx");
        response3xx = newCounter("responseCodes", monitorId, "code", "3xx");
        response4xx = newCounter("responseCodes", monitorId, "code", "4xx");
        response5xx = newCounter("responseCodes", monitorId, "code", "5xx");
    }

    public void update(int responseCode) {
        int firstDigit = responseCode / 100;
        switch (firstDigit) {
        case 1:
            response1xx.increment();
            break;
        case 2:
            response2xx.increment();
            break;
        case 3:
            response3xx.increment();
            break;
        case 4:
            response4xx.increment();
            break;
        case 5:
            response5xx.increment();
            break;
        }
    }

    public long getResponse1xx() {
        return response1xx.count();
    }

    public long getResponse2xx() {
        return response2xx.count();
    }

    public long getResponse3xx() {
        return response3xx.count();
    }

    public long getResponse4xx() {
        return response4xx.count();
    }

    public long getResponse5xx() {
        return response5xx.count();
    }
}
