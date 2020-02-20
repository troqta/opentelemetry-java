/*
 * Copyright 2020, OpenTelemetry Authors
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

package io.opentelemetry.sdk.metrics;

import io.opentelemetry.metrics.Counter;
import io.opentelemetry.sdk.metrics.common.InstrumentType;
import io.opentelemetry.sdk.metrics.common.InstrumentValueType;
import io.opentelemetry.sdk.metrics.data.MetricData.Descriptor;
import io.opentelemetry.sdk.metrics.view.Aggregation;
import io.opentelemetry.sdk.metrics.view.Aggregations;

abstract class AbstractCounter<B extends AbstractBoundInstrument>
    extends AbstractInstrumentWithBinding<B> {
  private final boolean monotonic;
  private final InstrumentValueType instrumentValueType;

  AbstractCounter(
      InstrumentDescriptor descriptor,
      InstrumentValueType instrumentValueType,
      MeterProviderSharedState meterProviderSharedState,
      MeterSharedState meterSharedState,
      boolean monotonic) {
    super(
        descriptor,
        meterProviderSharedState,
        meterSharedState,
        new ActiveBatcher(
            getDefaultBatcher(
                descriptor,
                getCounterInstrumentType(monotonic),
                instrumentValueType,
                meterProviderSharedState,
                meterSharedState)));
    this.monotonic = monotonic;
    this.instrumentValueType = instrumentValueType;
  }

  final boolean isMonotonic() {
    return monotonic;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AbstractCounter<?>)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    AbstractCounter<?> that = (AbstractCounter<?>) o;

    return monotonic == that.monotonic && instrumentValueType == that.instrumentValueType;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (monotonic ? 1 : 0);
    result = 31 * result + instrumentValueType.hashCode();
    return result;
  }

  abstract static class Builder<B extends Counter.Builder<B, V>, V>
      extends AbstractInstrument.Builder<B, V> implements Counter.Builder<B, V> {
    private boolean monotonic = true;

    Builder(
        String name,
        MeterProviderSharedState meterProviderSharedState,
        MeterSharedState meterSharedState) {
      super(name, meterProviderSharedState, meterSharedState);
    }

    @Override
    public final B setMonotonic(boolean monotonic) {
      this.monotonic = monotonic;
      return getThis();
    }

    final boolean isMonotonic() {
      return this.monotonic;
    }
  }

  private static InstrumentType getCounterInstrumentType(boolean monotonic) {
    return monotonic ? InstrumentType.COUNTER_MONOTONIC : InstrumentType.COUNTER_NON_MONOTONIC;
  }

  private static Batcher getDefaultBatcher(
      InstrumentDescriptor descriptor,
      InstrumentType instrumentType,
      InstrumentValueType instrumentValueType,
      MeterProviderSharedState meterProviderSharedState,
      MeterSharedState meterSharedState) {
    Aggregation defaultAggregation = Aggregations.sum();
    return Batchers.getCumulativeAllLabels(
        getDefaultMetricDescriptor(
            descriptor, instrumentType, instrumentValueType, defaultAggregation),
        meterProviderSharedState.getResource(),
        meterSharedState.getInstrumentationLibraryInfo(),
        defaultAggregation.getAggregatorFactory(instrumentValueType),
        meterProviderSharedState.getClock());
  }

  private static Descriptor getDefaultMetricDescriptor(
      InstrumentDescriptor descriptor,
      InstrumentType instrumentType,
      InstrumentValueType instrumentValueType,
      Aggregation aggregation) {
    return Descriptor.create(
        descriptor.getName(),
        descriptor.getDescription(),
        aggregation.getUnit(descriptor.getUnit()),
        aggregation.getDescriptorType(instrumentType, instrumentValueType),
        descriptor.getConstantLabels());
  }
}
