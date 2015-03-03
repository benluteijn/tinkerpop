/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tinkerpop.gremlin.hadoop.process.computer.spark;

import org.apache.spark.Accumulator;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.tinkerpop.gremlin.hadoop.process.computer.util.Rule;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.MapReduce;
import org.apache.tinkerpop.gremlin.process.computer.Memory;
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.util.MemoryHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public final class SparkMemory implements Memory.Admin, Serializable {

    public final Set<String> memoryKeys = new HashSet<>();
    private final AtomicInteger iteration = new AtomicInteger(0);
    private final AtomicLong runtime = new AtomicLong(0l);
    private final Map<String, Accumulator<Rule>> memory = new HashMap<>();

    public SparkMemory(final VertexProgram<?> vertexProgram, final Set<MapReduce> mapReducers, final JavaSparkContext sparkContext) {
        if (null != vertexProgram) {
            for (final String key : vertexProgram.getMemoryComputeKeys()) {
                MemoryHelper.validateKey(key);
                this.memoryKeys.add(key);
            }
        }
        for (final MapReduce mapReduce : mapReducers) {
            this.memoryKeys.add(mapReduce.getMemoryKey());
        }
        for (final String key : this.memoryKeys) {
            this.memory.put(key, sparkContext.accumulator(new Rule(Rule.Operation.NO_OP, null), new RuleAccumulator()));
        }

    }

    @Override
    public Set<String> keys() {
        final Set<String> trueKeys = new HashSet<>();
        this.memory.forEach((key, value) -> {
            if (value.value().object != null)
                trueKeys.add(key);
        });
        return Collections.unmodifiableSet(trueKeys);
    }

    @Override
    public void incrIteration() {
        this.iteration.getAndIncrement();
    }

    @Override
    public void setIteration(final int iteration) {
        this.iteration.set(iteration);
    }

    @Override
    public int getIteration() {
        return this.iteration.get();
    }

    @Override
    public void setRuntime(final long runTime) {
        this.runtime.set(runTime);
    }

    @Override
    public long getRuntime() {
        return this.runtime.get();
    }

    @Override
    public boolean isInitialIteration() {
        return this.getIteration() == 0;
    }

    @Override
    public <R> R get(final String key) throws IllegalArgumentException {
        final R r = (R) this.memory.get(key).value().object;
        if (null == r)
            throw Memory.Exceptions.memoryDoesNotExist(key);
        else
            return r;
    }

    @Override
    public long incr(final String key, final long delta) {
        checkKeyValue(key, delta);
        this.memory.get(key).add(new Rule(Rule.Operation.INCR, delta));
        return (Long) this.memory.get(key).value().object + delta;
    }

    @Override
    public boolean and(final String key, final boolean bool) {
        checkKeyValue(key, bool);
        this.memory.get(key).add(new Rule(Rule.Operation.AND, bool));
        return bool;
    }

    @Override
    public boolean or(final String key, final boolean bool) {
        checkKeyValue(key, bool);
        this.memory.get(key).add(new Rule(Rule.Operation.OR, bool));
        return bool;
    }

    @Override
    public void set(final String key, final Object value) {
        checkKeyValue(key, value);
        this.memory.get(key).add(new Rule(Rule.Operation.SET, value));
    }

    @Override
    public String toString() {
        return StringFactory.memoryString(this);
    }

    private void checkKeyValue(final String key, final Object value) {
        if (!this.memoryKeys.contains(key))
            throw GraphComputer.Exceptions.providedKeyIsNotAMemoryComputeKey(key);
        MemoryHelper.validateValue(value);
    }
}