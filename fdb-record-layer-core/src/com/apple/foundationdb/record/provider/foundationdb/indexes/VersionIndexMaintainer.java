/*
 * VersionIndexMaintainer.java
 *
 * This source file is part of the FoundationDB open source project
 *
 * Copyright 2015-2018 Apple Inc. and the FoundationDB project authors
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

package com.apple.foundationdb.record.provider.foundationdb.indexes;

import com.apple.foundationdb.MutationType;
import com.apple.foundationdb.record.IndexEntry;
import com.apple.foundationdb.record.IndexScanType;
import com.apple.foundationdb.record.RecordCoreException;
import com.apple.foundationdb.record.RecordCursor;
import com.apple.foundationdb.record.ScanProperties;
import com.apple.foundationdb.record.TupleRange;
import com.apple.foundationdb.record.metadata.MetaDataException;
import com.apple.foundationdb.record.metadata.expressions.VersionKeyExpression;
import com.apple.foundationdb.record.provider.foundationdb.FDBRecordVersion;
import com.apple.foundationdb.record.provider.foundationdb.FDBStoreTimer;
import com.apple.foundationdb.record.provider.foundationdb.FDBStoredRecord;
import com.apple.foundationdb.record.provider.foundationdb.IndexMaintainer;
import com.apple.foundationdb.record.provider.foundationdb.IndexMaintainerState;
import com.apple.foundationdb.tuple.Tuple;
import com.google.protobuf.Message;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Implementation of {@link IndexMaintainer} for the "version" index type. Here, the keys of the index are
 * required to have exactly one column that is of type {@link VersionKeyExpression Version}.
 * If records are added to this index with a complete {@link FDBRecordVersion} associated
 * with it, then the given version is used. If it is associated with an incomplete <code>RecordVersion</code>,
 * then it will set the key in such a way that the global version is filled in when the record is committed.
 * If one only ever serializes records with incomplete <code>RecordVersion</code>s, then this index guarantees
 * that the version column is strictly monotonically increasing with time.
 *
 * @param <M> type used to represent stored records
 */
public class VersionIndexMaintainer<M extends Message> extends StandardIndexMaintainer<M> {
    protected VersionIndexMaintainer(IndexMaintainerState<M> state) {
        super(state);
    }

    @Override
    @Nonnull
    public RecordCursor<IndexEntry> scan(@Nonnull IndexScanType scanType, @Nonnull TupleRange range,
                                            @Nullable byte[] continuation,
                                            @Nonnull ScanProperties scanProperties) {
        if (scanType != IndexScanType.BY_VALUE) {
            throw new RecordCoreException("Can only scan version index by value.");
        }
        return scan(range, continuation, scanProperties);
    }

    // Called by updateIndexKeys in StandardIndexMaintainer.
    @Override
    protected void updateOneKey(@Nonnull final FDBStoredRecord<M> savedRecord,
                                final boolean remove,
                                @Nonnull final IndexEntry indexEntry) {
        if (state.index.isUnique()) {
            throw new MetaDataException(String.format("%s index %s does not support unique indexes", state.index.getType(), state.index.getName()));
        }
        final Tuple valueKey = indexEntry.getKey();
        final Tuple value = indexEntry.getValue();
        final long startTime = System.nanoTime();
        final Tuple entryKey = indexEntryKey(valueKey, savedRecord.getPrimaryKey());
        final boolean hasIncomplete = entryKey.hasIncompleteVersionstamp();
        final byte[] keyBytes;
        if (hasIncomplete) {
            keyBytes = state.indexSubspace.packWithVersionstamp(entryKey);
        } else {
            keyBytes = state.indexSubspace.pack(entryKey);
        }
        if (remove) {
            if (hasIncomplete) {
                state.context.removeVersionMutation(keyBytes);
            } else {
                state.transaction.clear(state.indexSubspace.pack(entryKey));
            }
            if (state.store.getTimer() != null) {
                state.store.getTimer().recordSinceNanoTime(FDBStoreTimer.Events.DELETE_INDEX_ENTRY, startTime);
            }
        } else {
            final byte[] valueBytes = value.pack();
            checkKeyValueSizes(savedRecord, valueKey, value, keyBytes, valueBytes);
            if (hasIncomplete) {
                state.context.addVersionMutation(MutationType.SET_VERSIONSTAMPED_KEY, keyBytes, valueBytes);
            } else {
                state.transaction.set(keyBytes, valueBytes);
            }
            if (state.store.getTimer() != null) {
                state.store.getTimer().recordSinceNanoTime(FDBStoreTimer.Events.SAVE_INDEX_ENTRY, startTime);
            }
        }
    }
}