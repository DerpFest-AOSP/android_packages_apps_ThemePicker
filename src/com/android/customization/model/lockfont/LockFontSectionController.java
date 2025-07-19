/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.customization.model.lockfont;

import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager.Callback;
import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.model.CustomizationOption;
import com.android.customization.picker.lockfont.LockFontFragment;
import com.android.customization.picker.lockfont.LockFontSectionView;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.OptionSelectorController.OptionSelectedListener;
import com.android.themepicker.R;
import com.android.wallpaper.model.CustomizationSectionController;
import com.android.wallpaper.util.LaunchUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/** A {@link CustomizationSectionController} for lockscreen fonts. */

public class LockFontSectionController implements CustomizationSectionController<LockFontSectionView> {

    private static final String TAG = "LockFontSectionController";

    private final LockFontManager mFontOptionsManager;
    private final CustomizationSectionNavigationController mSectionNavigationController;
    private final boolean mIsDisabled;
    private LockFontSectionView mSectionView;
    private TextView mSectionDescription;
    private View mSectionTile;
    private Context mContext;
    private ContentObserver mClockFaceObserver;
    private final Callback mApplyFontCallback = new Callback() {
        @Override
        public void onSuccess() {
        }

        @Override
        public void onError(@Nullable Throwable throwable) {
        }
    };

    public LockFontSectionController(LockFontManager fontOptionsManager,
            CustomizationSectionNavigationController sectionNavigationController) {
        this(fontOptionsManager, sectionNavigationController, false);
    }

    public LockFontSectionController(LockFontManager fontOptionsManager,
            CustomizationSectionNavigationController sectionNavigationController,
            boolean isDisabled) {
        mFontOptionsManager = fontOptionsManager;
        mSectionNavigationController = sectionNavigationController;
        mIsDisabled = isDisabled;
    }

    @Override
    public boolean isAvailable(Context context) {
        return mFontOptionsManager.isAvailable();
    }

    @Override
    public LockFontSectionView createView(Context context) {
        mContext = context;
        mSectionView = (LockFontSectionView) LayoutInflater.from(context)
                .inflate(R.layout.lockfont_section_view, /* root= */ null);

        mSectionDescription = mSectionView.findViewById(R.id.font_section_description);
        mSectionTile = mSectionView.findViewById(R.id.font_section_tile);

        // Create and register ContentObserver to watch for clock face changes
        mClockFaceObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                super.onChange(selfChange, uri);
                if (uri != null && "lock_screen_custom_clock_face".equals(uri.getLastPathSegment())) {
                    updateSectionState();
                }
            }
        };
        
        context.getContentResolver().registerContentObserver(
                Settings.Secure.getUriFor("lock_screen_custom_clock_face"),
                false, mClockFaceObserver);

        updateSectionState();

        return mSectionView;
    }

    private void updateSectionState() {
        if (mSectionView == null || mContext == null) return;

        boolean isCustomClockSelected = isCustomClockSelected();
        
        if (isCustomClockSelected) {
            // When disabled, show disabled summary and make the view non-clickable
            mSectionDescription.setText(R.string.lockfont_disabled_summary);
            mSectionDescription.setAlpha(0.6f);
            mSectionTile.setAlpha(0.6f);
            mSectionView.setClickable(false);
            mSectionView.setEnabled(false);
            mSectionView.setAlpha(0.6f);
        } else {
            // Normal behavior when enabled
            mSectionDescription.setAlpha(1.0f);
            mSectionTile.setAlpha(1.0f);
            mSectionView.setClickable(true);
            mSectionView.setEnabled(true);
            mSectionView.setAlpha(1.0f);

            mFontOptionsManager.fetchOptions(new OptionsFetchedListener<LockFontOption>() {
                @Override
                public void onOptionsLoaded(List<LockFontOption> options) {
                    LockFontOption activeOption = getActiveOption(options);
                    mSectionDescription.setText(activeOption.getTitle());
                    activeOption.bindThumbnailTile(mSectionTile);
                }

                @Override
                public void onError(@Nullable Throwable throwable) {
                    if (throwable != null) {
                        Log.e(TAG, "Error loading font options", throwable);
                    }
                    mSectionDescription.setText(R.string.something_went_wrong);
                    mSectionTile.setVisibility(View.GONE);
                }
            }, /* reload= */ true);

            mSectionView.setOnClickListener(v -> mSectionNavigationController.navigateTo(
                    LockFontFragment.newInstance(mContext.getString(R.string.preview_name_lockfont))));
        }
    }

    private boolean isCustomClockSelected() {
        if (mContext == null) return false;
        
        String clockFaceJson = Settings.Secure.getString(
                mContext.getContentResolver(), "lock_screen_custom_clock_face");

        if (clockFaceJson == null || clockFaceJson.isEmpty()) {
            return false;
        }

        try {
            JSONObject clockFace = new JSONObject(clockFaceJson);
            return clockFace.has("clockId") && !"DEFAULT".equals(clockFace.optString("clockId"));
        } catch (JSONException e) {
            Log.w(TAG, "Failed to parse lock_screen_custom_clock_face: " + clockFaceJson, e);
            return false;
        }
    }

    private LockFontOption getActiveOption(List<LockFontOption> options) {
        return options.stream()
                .filter(option -> mFontOptionsManager.isActive(option))
                .findAny()
                // For development only, as there should always be a grid set.
                .orElse(options.get(0));
    }

    @Override
    public void release() {
        cleanup();
        mContext = null;
        mSectionView = null;
        mSectionDescription = null;
        mSectionTile = null;
    }

    public void cleanup() {
        if (mContext != null && mClockFaceObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mClockFaceObserver);
            mClockFaceObserver = null;
        }
    }
}
