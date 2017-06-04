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

import android.support.annotation.DrawableRes;
import android.support.design.widget.FloatingActionButton;
import android.view.View;

/**
 * @author Jin, Heonkyu <heonkyu.jin@gmail.com> on 2017. 6. 1.
 */

public class FloatingActionButtonManager {
    private FloatingActionButton mFAB;
    @DrawableRes
    private int mInitialDrawable;
    private View.OnClickListener mInitialOnClickListener;

    public FloatingActionButtonManager(FloatingActionButton fab, @DrawableRes int drawable,
                                       View.OnClickListener onClickListener) {
        mFAB = fab;
        mInitialDrawable = drawable;
        mInitialOnClickListener = onClickListener;

        changeImageResource(mInitialDrawable);
        changeOnClickListener(mInitialOnClickListener);
    }

    public FloatingActionButton getFloatingActionButton() {
        return mFAB;
    }

    public int getInitialDrawable() {
        return mInitialDrawable;
    }

    public void changeImageResource(@DrawableRes int drawable) {
        mFAB.setImageResource(drawable);
    }

    public void changeOnClickListener(View.OnClickListener onClickListener) {
        mFAB.setOnClickListener(onClickListener);
    }

    public void revertToInitialState() {
        changeImageResource(mInitialDrawable);
        changeOnClickListener(mInitialOnClickListener);
    }
}
