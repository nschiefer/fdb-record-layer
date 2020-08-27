/*
 * ExplodeExpression.java
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

package com.apple.foundationdb.record.query.plan.temp.expressions;

import com.apple.foundationdb.annotation.API;
import com.apple.foundationdb.record.query.plan.temp.AliasMap;
import com.apple.foundationdb.record.query.plan.temp.CorrelationIdentifier;
import com.apple.foundationdb.record.query.plan.temp.Quantifier;
import com.apple.foundationdb.record.query.plan.temp.RelationalExpression;
import com.google.common.collect.ImmutableSet;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A table function expression that "explodes" a repeated field into a stream of its values.
 */
@API(API.Status.EXPERIMENTAL)
public class ExplodeExpression implements RelationalExpression {
    @Nonnull
    private final CorrelationIdentifier correlationIdentifier;
    @Nonnull
    private final List<String> fieldNames;

    public ExplodeExpression(@Nonnull final CorrelationIdentifier correlationIdentifier, @Nonnull final List<String> fieldNames) {
        this.correlationIdentifier = correlationIdentifier;
        this.fieldNames = fieldNames;
    }

    @Nonnull
    @Override
    public List<? extends Quantifier> getQuantifiers() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public Set<CorrelationIdentifier> getCorrelatedTo() {
        return ImmutableSet.of(correlationIdentifier);
    }

    @Override
    public boolean equalsWithoutChildren(@Nonnull RelationalExpression otherExpression,
                                         @Nonnull final AliasMap equivalencesMap) {
        if (this == otherExpression) {
            return true;
        }
        return getClass() == otherExpression.getClass() &&
               fieldNames.equals(((ExplodeExpression)otherExpression).fieldNames);
    }

    @Override
    public int hashCodeWithoutChildren() {
        return 17;
    }

    @Nonnull
    @Override
    public RelationalExpression rebase(@Nonnull final AliasMap translationMap) {
        if (!translationMap.containsSource(correlationIdentifier)) {
            return this;
        } else {
            return new ExplodeExpression(translationMap.translate(correlationIdentifier), fieldNames);
        }
    }
}
