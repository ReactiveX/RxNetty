/*
 * Copyright 2014 Netflix, Inc.
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
package io.reactivex.netty.metrics;

/**
 * @author Nitesh Kant
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractMetricsEvent<T extends Enum> implements MetricsEvent<T> {

    protected final T name;
    protected final boolean isTimed;
    protected final boolean isError;

    protected AbstractMetricsEvent(T name, boolean isTimed, boolean isError) {
        this.isTimed = isTimed;
        this.name = name;
        this.isError = isError;
    }

    @Override
    public T getType() {
        return name;
    }

    @Override
    public boolean isTimed() {
        return isTimed;
    }

    @Override
    public boolean isError() {
        return isError;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AbstractMetricsEvent)) {
            return false;
        }

        AbstractMetricsEvent that = (AbstractMetricsEvent) o;

        return isError == that.isError && isTimed == that.isTimed && name == that.name;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (isTimed ? 1 : 0);
        result = 31 * result + (isError ? 1 : 0);
        return result;
    }
}
