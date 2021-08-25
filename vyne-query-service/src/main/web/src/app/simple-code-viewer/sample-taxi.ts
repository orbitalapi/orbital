export const sampleTaxi = `/**
 *this is a comment
*/
model TransactionProduct {
    baseSku: BaseSKU
    description: ProductDescription
    size: ProductSize
    color: ProductColor
}

[[ Models transactions as they occur in our order tracking system ]]
model OrderTransaction {
    [[ The products included in this transcation  ]]
    @SomeAnnotation
    products: TransactionProductDescription[]
    [[ The date and time of the transaction ]]
    time : TransactionTime inherits Instant
    [[ The email address of the purchasing customer ]]
    customer : CustomerEmailAddress
    [[ The value of the transaction ]]
    value : TransactionPrice inherits Decimal
}`;
