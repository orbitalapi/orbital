package io.vyne.cask.types

import lang.taxi.types.Field
import lang.taxi.types.ObjectType
import lang.taxi.types.Type

class TypeDiffer {

    fun compareType(oldVersion: Type, newVersion: Type): List<FieldDiff> {
        require(oldVersion::class == newVersion::class) { "The type of class has changed (from ${oldVersion::class.simpleName} to ${newVersion::class.simpleName}), this isn't currently supported" }
        return when (oldVersion) {
            is ObjectType -> diffObjectTypes(oldVersion, newVersion as ObjectType)
            else -> TODO("Not handled diffing ${oldVersion::class.simpleName}")
        }
    }

    private fun diffObjectTypes(oldVersion: ObjectType, newVersion: ObjectType): List<FieldDiff> {
        val removed = findFieldsPresentOnlyInBaseline(oldVersion, newVersion).map { FieldRemoved(it.name, it) }
        val added = findFieldsPresentOnlyInBaseline(newVersion, oldVersion).map { FieldAdded(it.name, it) }
        val changed = findFieldsPresentInBoth(oldVersion, newVersion)
                .mapNotNull { findChangedFields(it, oldVersion, newVersion) }
        return removed + added + changed
    }

    private fun findChangedFields(fieldName: String, oldVersion: ObjectType, newVersion: ObjectType): FieldChanged? {
        val oldField = oldVersion.field(fieldName)
        val newField = newVersion.field(fieldName)

        return if (oldField.type !== newField.type
                || oldField.nullable != newField.nullable
                || oldField.accessor != newField.accessor
                || oldField.readExpression != newField.readExpression
        ) {
           FieldChanged(fieldName, oldField, newField)
        } else {
            null
        }
    }

    private fun findFieldsPresentInBoth(baseLine: ObjectType, delta: ObjectType): List<String> {
        return baseLine.allFields.filter { delta.hasField(it.name) }
                .map { it.name }
    }

    private fun findFieldsPresentOnlyInBaseline(baseLine: ObjectType, delta: ObjectType): List<Field> {
        return baseLine.allFields.filter { !delta.hasField(it.name) }
    }

}

sealed class FieldDiff {
    abstract val fieldName: String
}

data class Unchanged(override val fieldName: String) : FieldDiff()
data class FieldAdded(override val fieldName: String, val newVersion: Field) : FieldDiff()
data class FieldRemoved(override val fieldName: String, val oldVersion: Field) : FieldDiff()
data class FieldChanged(override val fieldName: String, val newVersion: Field, val oldVersion: Field) : FieldDiff()
