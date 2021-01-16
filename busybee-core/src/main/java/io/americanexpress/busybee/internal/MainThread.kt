/*
 * Copyright 2019 American Express Travel Related Services Company, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.americanexpress.busybee.internal

import java.util.concurrent.Executor
import java.util.concurrent.Executors

object MainThread {
    // Double check locking https://errorprone.info/bugpattern/DoubleCheckedLocking
    @Volatile
    private var INSTANCE: Executor? = null
    private fun jvmExecutor(): Executor? {
        var instance = INSTANCE
        if (instance == null) {
            synchronized(MainThread::class.java) {
                instance = INSTANCE
                if (instance == null) {
                    INSTANCE = Executors.newSingleThreadExecutor()
                }
            }
        }
        return INSTANCE
    }

    fun singletonExecutor(): Executor? {
        /*
         * Can only load AndroidMainThreadExecutor if we are on Android (not JVM)
         * else will we get class not found errors.
         */
        return if (EnvironmentChecks.hasWorkingAndroidMainLooper()) {
            val androidExecutorClass = Reflection.clazz(
                "io.americanexpress.busybee.android.internal.AndroidMainThreadExecutor",
                "Must add busybee-android dependency when running on Android"
            )
            val instance = Reflection.getField(androidExecutorClass, "INSTANCE")
            Reflection.getValue(instance) as Executor
        } else {
            jvmExecutor()
        }
    }
}