package io.vyne.cask.format.csv

import io.vyne.schemas.Path
import io.vyne.schemas.QualifiedName
import io.vyne.schemas.taxi.TaxiSchema
import io.vyne.cask.types.TypeDiffer

class CsvUpdgraderPipeline(
        val name:QualifiedName,
        val baseSchema:TaxiSchema,
        val targetSchema: TaxiSchema,
        val readCache:Path
) {
    val baseVyneType = baseSchema.type(name)
    val targetVyneType  = targetSchema.type(name)
    val baseTaxiType = baseSchema.taxi.type(name.fullyQualifiedName)
    val targetTaxiType = targetSchema.taxi.type(name.fullyQualifiedName)
    val diff = TypeDiffer().compareType(baseTaxiType,targetTaxiType)
}
