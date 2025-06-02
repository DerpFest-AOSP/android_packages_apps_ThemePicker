/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.customization.picker.icon.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import com.android.customization.module.CustomizationPreferences
import com.android.themepicker.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.module.InjectorProvider
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ThemePickerIconStyleRepository
@Inject
constructor(
    @ApplicationContext private val appContext: Context,
    private val contentResolver: ContentResolver,
    @BackgroundDispatcher private val backgroundScope: CoroutineScope,
) : IconStyleRepository {
    private val metadataKey = appContext.getString(R.string.themed_icon_metadata_key)
    private var previewUtils: PreviewUtils? = null
    private val previewUtilsFlow = flow {
        // If PreviewUtils is created too early on start up, the provider (e.g. Launcher) may not be
        // ready, so PreviewUtils#supportsPreview would return false. Only cache previewUtils if it
        // supports previewing. Otherwise, retry when new flow consumers appear.
        if (previewUtils == null) {
            PreviewUtils(appContext, metadataKey, Screen.HOME_SCREEN).let {
                if (it.supportsPreview()) {
                    previewUtils = it
                }
            }
        }
        emit(previewUtils)
    }
    private var uri: Uri? = null
    private val uriFlow: Flow<Uri?> =
        previewUtilsFlow.map { uri ?: it?.getUri(ICON_THEMED)?.also { result -> uri = result } }

    override val isThemedIconAvailable: Flow<Boolean> = previewUtilsFlow.map { it != null }

    override val isThemedIconActivated: Flow<Boolean> =
        uriFlow
            .flatMapLatest {
                callbackFlow {
                    var disposableHandle: DisposableHandle? = null
                    if (it != null) {
                        val contentObserver =
                            object : ContentObserver(null) {
                                override fun onChange(selfChange: Boolean) {
                                    trySend(getThemedIconEnabled(it))
                                }
                            }
                        contentResolver.registerContentObserver(
                            it,
                            /* notifyForDescendants= */ true,
                            contentObserver,
                        )

                        trySend(getThemedIconEnabled(it))

                        disposableHandle = DisposableHandle {
                            contentResolver.unregisterContentObserver(contentObserver)
                        }
                    }
                    awaitClose { disposableHandle?.dispose() }
                }
            }
            .stateIn(
                scope = backgroundScope,
                started = SharingStarted.WhileSubscribed(),
                initialValue = false,
            )

    fun getThemedIconEnabled(uri: Uri): Boolean {
        val cursor =
            contentResolver.query(
                uri,
                /* projection= */ null,
                /* selection= */ null,
                /* selectionArgs= */ null,
                /* sortOrder= */ null,
            )
        var isEnabled = false
        if (cursor != null && cursor.moveToNext()) {
            isEnabled = (cursor.getInt(cursor.getColumnIndex(COL_ICON_THEMED_VALUE)) == ENABLED)
            val preferences =
                InjectorProvider.getInjector().getPreferences(appContext)
                    as CustomizationPreferences
            if (preferences.getThemedIconEnabled() != isEnabled) {
                preferences.setThemedIconEnabled(isEnabled)
            }
        }
        cursor?.close()
        return isEnabled
    }

    override suspend fun setThemedIconEnabled(enabled: Boolean) {
        uri?.let {
            val values = ContentValues()
            values.put(COL_ICON_THEMED_VALUE, enabled)
            contentResolver.update(it, values, /* where= */ null, /* selectionArgs= */ null)
        }
    }

    companion object {
        private const val ICON_THEMED = "icon_themed"
        private const val COL_ICON_THEMED_VALUE = "boolean_value"
        private const val ENABLED = 1
    }
}
