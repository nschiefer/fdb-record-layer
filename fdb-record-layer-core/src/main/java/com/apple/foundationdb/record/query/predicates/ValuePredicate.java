/*
 * ValuePredicate.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2015-2020 Apple Inc. and the FoundationDB project authors
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

package com.apple.foundationdb.record.query.predicates;

import com.apple.foundationdb.annotation.API;
import com.apple.foundationdb.record.EvaluationContext;
import com.apple.foundationdb.record.PlanHashable;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordStoreBase;
import com.apple.foundationdb.record.query.expressions.Comparisons;
import com.apple.foundationdb.record.query.plan.temp.AliasMap;
import com.apple.foundationdb.record.query.plan.temp.Bindable;
import com.apple.foundationdb.record.query.plan.temp.CorrelationIdentifier;
import com.apple.foundationdb.record.query.plan.temp.matchers.ExpressionMatcher;
import com.apple.foundationdb.record.query.plan.temp.matchers.PlannerBindings;
import com.apple.foundationdb.record.query.plan.temp.view.SourceEntry;
import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A predicate consisting of a {@link Value} and a {@link com.apple.foundationdb.record.query.expressions.Comparisons.Comparison}.
 */
@API(API.Status.EXPERIMENTAL)
public class ValuePredicate implements QueryPredicate {
    @Nonnull
    private final Value value;
    @Nonnull
    private final Comparisons.Comparison comparison;

    public ValuePredicate(@Nonnull Value value, @Nonnull Comparisons.Comparison comparison) {
        this.value = value;
        this.comparison = comparison;
    }

    @Nonnull
    public Comparisons.Comparison getComparison() {
        return comparison;
    }

    @Nonnull
    public Value getValue() {
        return value;
    }

    @Nullable
    @Override
    public <M extends Message> Boolean eval(@Nonnull final FDBRecordStoreBase<M> store, @Nonnull final EvaluationContext context, @Nonnull final SourceEntry sourceEntry) {
        throw new UnsupportedOperationException();
        //return comparison.eval(store, context, value.eval(sourceEntry));
    }

    @Nonnull
    @Override
    public Set<CorrelationIdentifier> getCorrelatedTo() {
        return value.getCorrelatedTo(); // TODO add parameter correlations once we have them
    }

    @Nonnull
    @Override
    public QueryPredicate rebase(@Nonnull final AliasMap translationMap) {
        Value rebasedValue = value.rebase(translationMap);
        // TODO rebase comparison if needed
        if (value != rebasedValue) {
            return new ValuePredicate(rebasedValue, comparison);
        }
        return this;
    }

    @Nonnull
    @Override
    public Stream<PlannerBindings> bindTo(@Nonnull final ExpressionMatcher<? extends Bindable> matcher) {
        return matcher.matchWith(this);
    }

    @Override
    public boolean semanticEquals(@Nullable final Object other, @Nonnull final AliasMap equivalenceMap) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final ValuePredicate that = (ValuePredicate)other;
        return value.semanticEquals(that.value, equivalenceMap) &&
               comparison.equals(that.comparison); // TODO do semanticEquals on comparison
    }

    @Override
    public int semanticHashCode() {
        return Objects.hash(value.semanticHashCode(), comparison); // TODO semanticHashCode for comparison
    }

    @Override
    public int planHash() {
        return PlanHashable.planHash(value, comparison);
    }
}
