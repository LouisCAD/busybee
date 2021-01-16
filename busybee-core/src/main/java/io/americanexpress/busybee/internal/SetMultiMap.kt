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

import java.util.Collections
import java.util.HashMap
import java.util.HashSet

/**
 * Simple purpose-built SetMultiMap designed for the needs of BusyBee.
 * We didn't want to have BusyBee depend on guava, so Guava's SetMultiMap wasn't an option.
 * Builds on top of HashMap/HashSet's internally.
 *
 *
 * This collection maps keys of type K to sets of values of type V.
 * Each value is unique across all keys in the collection.
 *
 *
 * Not the most efficient impl, but good enough for the BusyBee use case.
 *
 *
 * WARNING: this Impl has some weird quirks, so probably not good for general purpose use.
 * Biggest quirk is the at each value is unique across all keys.
 * if you want to change the key for a value, you must remove it and re-add it.
 *
 *
 * See the method JavaDocs for details.
 */
class SetMultiMap<K, V> {
    private val map = HashMap<K?, MutableSet<V>>()
    private val reverseMap = HashMap<V?, K?>()

    /**
     * All keys are unique and all values are unique.
     * If the Value already exists under any Key,
     * then it will not be added again.
     *
     *
     * WARNING:
     * To re-associate with another Key, first `removeValue` then re-add the Value
     * Else this method will throw an exception.
     *
     * @return true if a new entry was added.
     * @throws IllegalStateException when you add the SAME value again with a DIFFERENT key.
     */
    @Throws(IllegalStateException::class)
    fun add(key: K, value: V): Boolean {
        check(!(reverseMap[value] != null && reverseMap[value] !== key)) {
            """You can't insert the same value for 2 different keys.
This mapping already exists: 
'${reverseMap[value]}' =>
  '$value'
but you tried to add this new mapping: 
'$key' =>
  '$value'
Remove the old mapping first!"""
        }
        if (reverseMap[value] === key) {
            return false
        }
        var valueSet = map[key]
        if (valueSet == null) {
            valueSet = HashSet()
            map[key] = valueSet
        }
        valueSet.add(value)
        reverseMap[value] = key
        return true
    }

    /**
     * Each value ONLY appears once, so it is associated with at most one key.
     *
     * @return key for which the value is associated with.
     */
    fun keyFor(value: V): K? {
        return reverseMap[value]
    }

    /**
     * Removes the value if and only if it exists in the map.
     *
     * @return true if an entry was removed.
     */
    fun removeValue(value: V): Boolean {
        if (reverseMap.containsKey(value)) {
            val keyForRemoved = reverseMap.remove(value)
            map[keyForRemoved]!!.remove(value)
            return true
        }
        return false
    }

    /**
     * @return provides an iterator ( with remove support ) for all the values in the collection.
     */
    fun valuesIterator(): MutableIterator<V> {
        val valueIterator: MutableIterator<Map.Entry<V?, K?>> = reverseMap.entries.iterator()
        return multiMapIteratorFromReverseMapIterator(valueIterator)
    }

    private fun multiMapIteratorFromReverseMapIterator(valueIterator: MutableIterator<Map.Entry<V, K?>>): MutableIterator<V> {
        return object : MutableIterator<V> {
            private var mapEntry: Map.Entry<V, K?>? = null
            override fun hasNext(): Boolean {
                return valueIterator.hasNext()
            }

            override fun next(): V {
                mapEntry = valueIterator.next()
                return mapEntry.key
            }

            override fun remove() {
                valueIterator.remove()
                map[mapEntry!!.value]!!.remove(mapEntry!!.key)
            }
        }
    }

    private fun multiMapIteratorFromForwardMapIterator(valueIterator: Iterator<V>): MutableIterator<V> {
        return object : MutableIterator<V> {
            private var nextValue: V? = null
            override fun hasNext(): Boolean {
                return valueIterator.hasNext()
            }

            override fun next(): V {
                nextValue = valueIterator.next()
                return nextValue
            }

            override fun remove() {
                valueIterator.remove()
                reverseMap.remove(nextValue)
            }
        }
    }

    /**
     * @return All the keys that has ever been used in this map since its creation
     */
    fun allKeys(): Set<K> {
        return Collections.unmodifiableSet(map.keys)
    }

    /**
     * @return All values across all keys
     */
    fun allValues(): Set<V> {
        return Collections.unmodifiableSet(reverseMap.keys)
    }

    /**
     * @return Values for the given key
     */
    fun values(key: K): Set<V> {
        val values = map[key] ?: return emptySet()
        return Collections.unmodifiableSet(values)
    }

    /**
     * @return True if and only if there are no values ( there may be keys )
     */
    fun hasNoValues(): Boolean {
        return reverseMap.isEmpty()
    }

    fun valuesIterator(key: K): MutableIterator<V> {
        var values: Set<V>? = map[key]
        if (values == null) {
            values = emptySet()
        }
        val valueIterator = values.iterator()
        return multiMapIteratorFromForwardMapIterator(valueIterator)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("\n{\n")
        for ((key, value1) in map) {
            sb.append("'").append(key).append("'\n")
            val iterator = value1.iterator()
            while (iterator.hasNext()) {
                val value = iterator.next()
                if (iterator.hasNext()) {
                    sb.append(" ├─ '")
                } else {
                    sb.append(" └─ '")
                }
                sb.append(value).append("'\n")
            }
        }
        sb.append("}")
        return sb.toString()
    }
}