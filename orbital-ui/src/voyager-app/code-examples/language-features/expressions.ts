import {StubQueryMessageWithSlug} from "src/app/services/query.service";

export const Expressions: StubQueryMessageWithSlug = {
  "title": "Using expressions",
  "slug": "expressions",
  "query": {
    "schema": `namespace demo

type SubTotal inherits Decimal
type TaxRate inherits Decimal
type TotalPrice inherits Decimal

model Order {
   subtotal: SubTotal inherits Decimal
   taxRate: TaxRate inherits Decimal
   quantity: Quantity inherits Int
   itemCount: ItemCount inherits Int
}

model Person {
   firstName: FirstName inherits String
   lastName: LastName inherits String
}

model Circle {
   radius: Radius inherits Decimal
}

service OrderService {
   operation getOrders(): Order[]

}

service PersonService {
   operation getPeople(): Person[]
}`,
    "stubs": [
      {
        "operationName": "getOrders",
        "response": "[\n   { \"subtotal\": 100.00, \"taxRate\": 0.2, \"quantity\": 2, \"itemCount\": 4 },\n   { \"subtotal\": 50.00, \"taxRate\": 0.2, \"quantity\": 1, \"itemCount\": 2 }\n]"
      },
      {
        "operationName": "getPeople",
        "response": "[\n   { \"firstName\": \"John\", \"lastName\": \"Smith\" },\n   { \"firstName\": \"Jane\", \"lastName\": \"Doe\" }\n]\n"
      }
    ],
    "query": `find { Order[] } as {
   subtotal: SubTotal,
   taxRate: TaxRate,
   quantity: Quantity,

   // Calculate price per item including tax
   pricePerItem: TotalPrice = (this.subtotal * (1 + this.taxRate)) / this.quantity
}[]
`,
    "parameters": {},
    "readme": "# Working with Expressions in Taxi\n\nExpressions let you calculate values based on other fields or literals. They can be used inline in queries or defined in types.\n\n## Basic Arithmetic\n\n### Simple Calculations\n```taxiql\nfind { Order[] } as {\n   subtotal: SubTotal,\n   taxRate: TaxRate,\n   // Using a type as a variable in an expression \n   total: TotalPrice = SubTotal * (1 + TaxRate)\n}[]\n```\n\n### Using Field References\nThe same calculation using explicit field references:\n```taxiql\nfind { Order[] } as {\n   subtotal: SubTotal,\n   taxRate: TaxRate,\n   total: TotalPrice = this.subtotal * (1 + this.taxRate)\n}[]\n```\n\n## String Operations\n\n### String Concatenation\n```taxiql\nfind { Person[] } as {\n   firstName: FirstName,\n   lastName: LastName,\n   // Mixing field references and type references\n   fullName: String = concat(this.firstName, \" \", LastName)\n}[]\n```\n\n## Defining Reusable Expressions\n\n### Inline Expression\n```taxiql\ngiven { circle: Circle = { radius: 10.0 } }\nfind { circle } as {\n   radius: Radius,\n   area: Decimal = 3.14159 * (this.radius * this.radius)\n}\n```\n\n### Same Expression as a Type\n```taxiql\ntype CircleArea inherits Decimal = 3.14159 * (Radius * Radius)\n\ngiven { circle: Circle = { radius: 10.0 } }\nfind { circle } as {\n   radius: Radius,\n   area: CircleArea\n}\n```\n\n## Complex Examples\n\n### Multiple Operations\n```taxiql\nfind { Order[] } as {\n   subtotal: SubTotal,\n   taxRate: TaxRate,\n   quantity: Quantity,\n   \n   // Calculate price per item including tax\n   pricePerItem: TotalPrice = (this.subtotal * (1 + this.taxRate)) / this.quantity\n}[]\n```\n\n## Available Operators\n\n| Operator | Description | Example |\n|----------|-------------|---------|\n| + | Addition | `price + tax` |\n| - | Subtraction | `total - discount` |\n| * | Multiplication | `quantity * price` |\n| / | Division | `total / count` |\n| && | Logical AND | `price > 0 && quantity > 0` |\n| \\|\\| | Logical OR | `isVIP \\|\\| hasDiscount` |\n| > | Greater than | `price > 100` |\n| >= | Greater than or equal | `age >= 18` |\n| < | Less than | `stock < reorderPoint` |\n| <= | Less than or equal | `price <= maxPrice` |\n| == | Equal to | `status == \"ACTIVE\"` |\n| != | Not equal to | `type != \"DELETED\"` |\n\n## Common Functions\n\nTaxi ships with a full [stdlib](https://taxilang.org/docs/language/stdlib) of functions - here's a common few...\n\n| Function | Description | Example |\n|----------|-------------|---------|\n| concat | Join strings | `concat(firstName, \" \", lastName)` |\n| upperCase | Convert to upper case | `upperCase(name)` |\n| lowerCase | Convert to lower case | `lowerCase(code)` |\n| trim | Remove whitespace | `trim(input)` |"
  }
}
