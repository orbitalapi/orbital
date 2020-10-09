package io.vyne.models

import com.winterbe.expekt.should
import io.vyne.schemas.taxi.TaxiSchema
import org.junit.Test

// Tests focussed on parsing abstract models and their subtypes
class AbstractModelsTest {

   @Test
   fun canParseJsonContentToConcreteTypes() {
      val schema = TaxiSchema.from("""
          enum AnimalType {
               MAMMAL,
               REPTILE
            }
            abstract model Animal(animalType : AnimalType) {
               speciesName : SpeciesName as String
            }
            model Person inherits Animal( this.animalType = AnimalType.MAMMAL ) {
               favouriteCoffee : FavouriteCoffee as String
            }
            model LizardMan inherits Animal( this.animalType = AnimalType.REPTILE ) {
               height : Height as Int // Testing column with different meanings
            }
      """.trimIndent())
      val json = """
         [
            { "animalType" : "MAMMAL" , "speciesName" : "Hipster", "favouriteCoffee" : "Latte" },
            { "animalType" : "REPTILE" , "speciesName" : "Super hero lizard", "height" : 23 }
         ]
      """.trimIndent()
      val animals = TypedInstance.from(schema.type("Animal[]"), json, schema, source = Provided) as TypedCollection
      val person = animals[0] as TypedObject
      person.typeName.should.equal("Person")
      person["animalType"].value.should.equal("MAMMAL")
      person["speciesName"].value.should.equal("Hipster")
      person["favouriteCoffee"].value.should.equal("Latte")

      val lizard = animals[1] as TypedObject
      lizard.typeName.should.equal("LizardMan")
      lizard["animalType"].value.should.equal("REPTILE")
      lizard["speciesName"].value.should.equal("Super hero lizard")
      lizard["height"].value.should.equal(23)
   }

   @Test
   fun canParseCsvContentToConcreteTypes() {
      val schema = TaxiSchema.from("""
          enum AnimalType {
               MAMMAL,
               REPTILE
            }
            abstract model Animal(animalType : AnimalType by column(1)) {
               speciesName : SpeciesName as String by column(2)
            }
            model Person inherits Animal( this.animalType = AnimalType.MAMMAL ) {
               favouriteCoffee : FavouriteCoffee as String by column(3)
            }
            model LizardMan inherits Animal( this.animalType = AnimalType.REPTILE ) {
               height : Height as Int by column(3) // Testing column with different meanings
            }

            @CsvList
            type alias AnimalList as Animal[]
      """.trimIndent())
      val csv = """
         type,speciesName,stuff
         MAMMAL,Hipster,Latte
         REPTILE,Super hero lizard,23
      """.trimIndent()
      val animals = TypedInstance.from(schema.type("AnimalList"), csv, schema, source = Provided) as TypedCollection
      val person = animals[0] as TypedObject
      person.typeName.should.equal("Person")
      person["animalType"].value.should.equal("MAMMAL")
      person["speciesName"].value.should.equal("Hipster")
      person["favouriteCoffee"].value.should.equal("Latte")

      val lizard = animals[1] as TypedObject
      lizard.typeName.should.equal("LizardMan")
      lizard["animalType"].value.should.equal("REPTILE")
      lizard["speciesName"].value.should.equal("Super hero lizard")
      lizard["height"].value.should.equal(23)
   }
}
