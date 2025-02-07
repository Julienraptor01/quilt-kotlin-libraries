/*
 * Copyright 2022 The Quilt Project
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

package org.quiltmc.qkl.library.nbt

import net.minecraft.nbt.*
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * A property delegate for NBT tags.
 */
public typealias NbtProperty<T> = ReadWriteProperty<Any?, T>

/**
 * A provider for NBT property delegates.
 */
public typealias NbtPropertyProvider<T> = PropertyDelegateProvider<Any?, NbtProperty<T>>

/**
 * A reference to a [Compound][NbtCompound]. To get one, create a variable and reference it with `::variableName`.
 */
public typealias CompoundProperty = () -> NbtCompound

internal class Delegate<T>(
    val compound: CompoundProperty,
    val name: String,
    default: T? = null,
    val toNbt: (T) -> NbtElement,
    val fromNbt: (NbtElement) -> T,
) : NbtProperty<T> {
    var lastKnownValue = default

    init {
        if (default != null) {
            compound().put(name, toNbt(default))
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return when {
            name in compound() -> {
                val value = fromNbt(compound().get(name)!!)
                lastKnownValue = value
                value
            }
            lastKnownValue != null -> {
                compound().put(name, toNbt(lastKnownValue!!))
                lastKnownValue!!
            }
            else -> {
                error("No value for $name in ${compound()}")
            }
        }
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        compound().put(name, toNbt(value))
        lastKnownValue = value
    }
}

internal inline fun <T> provider(crossinline action: (String) -> NbtProperty<T>): NbtPropertyProvider<T> {
    return PropertyDelegateProvider { _, property -> property.name.let(action) }
}

/**
 * Get a copy of this variable, as a constant reference. Future changes to the variable will not affect the copy,
 * and therefore any delegations created from this copy will always point to the same compound.
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.Properties
 */
public fun CompoundProperty.constant(): CompoundProperty {
    val value = this()
    return { value }
}

/**
 * Get this compound as a [CompoundProperty]. This can be used to create delegations.
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.Properties
 */
public fun NbtCompound.property(): CompoundProperty {
    return { this }
}

/**
 * An integer in a [Compound][NbtCompound].
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.int(name: String? = null, default: Int? = null): NbtPropertyProvider<Int> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtInt::of) { (it as NbtInt).intValue() }
    }
}

/**
 * A long in a [Compound][NbtCompound].
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.long(name: String? = null, default: Long? = null): NbtPropertyProvider<Long> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtLong::of) { (it as NbtLong).longValue() }
    }
}

/**
 * A byte in a [Compound][NbtCompound].
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.byte(name: String? = null, default: Byte? = null): NbtPropertyProvider<Byte> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtByte::of) { (it as NbtByte).byteValue() }
    }
}

/**
 * A boolean in a [Compound][NbtCompound], internally stored as a byte.
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.boolean(name: String? = null, default: Boolean? = null): NbtPropertyProvider<Boolean> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtByte::of) { (it as NbtByte).byteValue() != 0.toByte() }
    }
}

/**
 * A short in a [Compound][NbtCompound].
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.short(name: String? = null, default: Short? = null): NbtPropertyProvider<Short> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtShort::of) { (it as NbtShort).shortValue() }
    }
}

/**
 * A float in a [Compound][NbtCompound].
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.float(name: String? = null, default: Float? = null): NbtPropertyProvider<Float> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtFloat::of) { (it as NbtFloat).floatValue() }
    }
}

/**
 * A double in a [Compound][NbtCompound].
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.double(name: String? = null, default: Double? = null): NbtPropertyProvider<Double> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtDouble::of) { (it as NbtDouble).doubleValue() }
    }
}

/**
 * A string in a [Compound][NbtCompound].
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.BasicTypes
 */
public fun CompoundProperty.string(name: String? = null, default: String? = null): NbtPropertyProvider<String> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, NbtString::of) { it.asString() }
    }
}

/**
 * A [Compound][NbtCompound] in a Compound. Compounds created in this way can also be used with all the other
 * delegating functions for compound properties if this is applied to a variable.
 *
 * @author sschr15
 * @sample samples.qkl.nbt.NbtSamples.Nesting
 */
public fun CompoundProperty.compound(
    name: String? = null,
    default: NbtCompound? = null
): NbtPropertyProvider<NbtCompound> {
    return provider { propName ->
        Delegate(this, name ?: propName, default, { it }) { it as NbtCompound }
    }
}
