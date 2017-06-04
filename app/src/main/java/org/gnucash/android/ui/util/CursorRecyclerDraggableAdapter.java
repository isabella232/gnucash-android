/*
 * Copyright (c) 2017 Jin, Heonkyu <heonkyu.jin@gmail.com>
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

package org.gnucash.android.ui.util;

import android.database.Cursor;
import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 5. 31.
 */

public abstract class CursorRecyclerDraggableAdapter<VH extends RecyclerView.ViewHolder>
        extends CursorRecyclerAdapter<VH> implements OnDragListener {
    private List<Integer> mIndex;
    private SparseArrayCompat<Boolean> mChanged = new SparseArrayCompat<>();

    private ItemTouchHelper mTouchHelper;

    public CursorRecyclerDraggableAdapter(Cursor cursor, RecyclerView recyclerView) {
        super(cursor);
        onCursorChange();

        ItemTouchHelperCallback callback = new ItemTouchHelperCallback();
        mTouchHelper = new ItemTouchHelper(callback);
        mTouchHelper.attachToRecyclerView(recyclerView);
    }

    private void onCursorChange() {
        Cursor cursor = getCursor();
        if (cursor != null) {
            int size = cursor.getCount();
            mIndex = new ArrayList<>(size);
            for (int i = 0; i < size; i++) mIndex.add(i);
            mChanged.clear();
        }
    }

    @Override
    public Cursor swapCursor(Cursor newCursor) {
        Cursor cursor = super.swapCursor(newCursor);
        onCursorChange();
        return cursor;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(getItemPosition(position));
    }

    @Override
    public void onBindViewHolder(VH holder, int i) {
        super.onBindViewHolder(holder, getItemPosition(i));
    }

    private int getItemPosition(int position) {
        return mIndex.get(position);
    }

    @Override
    public void onDragDismiss(RecyclerView.ViewHolder viewHolder) {
    }

    @Override
    public boolean onDragMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target) {
        int fromPosition = source.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mIndex, i, i + 1);
                mChanged.put(i, true);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mIndex, i, i - 1);
                mChanged.put(i, true);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
        int startPosition = Math.min(fromPosition, toPosition);
        int endPosition = Math.max(fromPosition, toPosition);
        notifyItemRangeChanged(startPosition, endPosition - startPosition + 1);

        return true;
    }

    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mTouchHelper.startDrag(viewHolder);
    }

    abstract public void onEndDrag();

    public List<UpdatedItem> getUpdatedItems() {
        List<UpdatedItem> updated = new ArrayList<>(mChanged.size());
        for (int i = 0; i < mIndex.size(); i++) {
            if (mChanged.get(i, false)) {
                updated.add(new UpdatedItem(getItemId(i), i, mIndex.get(i)));
            }
        }
        return updated;
    }

    private class ItemTouchHelperCallback extends ItemTouchHelper.Callback {
        @Override
        public boolean isLongPressDragEnabled() {
            return true;
        }

        @Override
        public int getMovementFlags(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
            return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
            return onDragMove(viewHolder, target);
        }

        @Override
        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            onDragDismiss(viewHolder);
        }
    }

    public static class UpdatedItem {
        public long itemId;
        public int oldPosition;
        public int newPosition;

        public UpdatedItem(long itemId, int oldPosition, int newPosition) {
            this.itemId = itemId;
            this.oldPosition = oldPosition;
            this.newPosition = newPosition;
        }
    }
}

interface OnDragListener {
    public void onDragDismiss(RecyclerView.ViewHolder viewHolder);
    public boolean onDragMove(RecyclerView.ViewHolder source, RecyclerView.ViewHolder target);
}