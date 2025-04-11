/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.customization.module.logging

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeAppSessionId @Inject constructor() : AppSessionId {
    override fun createNewId(): AppSessionId {
        return FakeAppSessionId()
    }

    override fun getId(): Int {
        return TEST_APP_SESSION_ID
    }

    companion object {
        const val TEST_APP_SESSION_ID = 1007
    }
}
