
export const sampleOrderEventType = {
  'name': {
    'fullyQualifiedName': 'cacib.orders.OrderEventType',
    'parameters': [],
    'name': 'OrderEventType',
    'namespace': 'cacib.orders',
    'parameterizedName': 'cacib.orders.OrderEventType',
    'shortDisplayName': 'OrderEventType',
    'longDisplayName': 'cacib.orders.OrderEventType'
  },
  'attributes': {},
  'modifiers': [
    'ENUM'
  ],
  'metadata': [],
  'aliasForType': null,
  'inheritsFrom': [],
  'enumValues': [
    {
      'name': 'Open',
      'value': 'Open',
      'synonyms': [
        'hpc.orders.EntryType.Opened',
        'cacib.cemafor.orders.EntryType.Opened'
      ],
      'typeDoc': ''
    },
    {
      'name': 'Filled',
      'value': 'Filled',
      'synonyms': [],
      'typeDoc': ''
    },
    {
      'name': 'Withheld',
      'value': 'Withheld',
      'synonyms': [
        'hpc.orders.EntryType.WithHeld',
        'cacib.cemafor.orders.EntryType.WithHeld'
      ],
      'typeDoc': ''
    }
  ],
  'sources': [
    {
      'name': 'hpc/Order.taxi',
      'version': '0.0.0',
      'content': 'Opened synonym of OrderEventType.Open',
      'id': 'hpc/Order.taxi:0.0.0'
    },
    {
      'name': 'cacib/orders/Order.taxi',
      'version': '0.0.0',
      'content': 'enum OrderEventType {\n   Open,\n   Filled,\n   Withheld\n}',
      'id': 'cacib/orders/Order.taxi:0.0.0'
    }
  ],
  'typeParameters': [],
  'typeDoc': 'This indicates the type of an order.  Orders have many flavours.  Strawberry, Chococlate, and Vanilla.',
  'isTypeAlias': false,
  'format': null,
  'hasFormat': false,
  'isParameterType': false,
  'isClosed': false,
  'isPrimitive': false,
  'fullyQualifiedName': 'cacib.orders.OrderEventType',
  'memberQualifiedName': {
    'fullyQualifiedName': 'cacib.orders.OrderEventType',
    'parameters': [],
    'name': 'OrderEventType',
    'namespace': 'cacib.orders',
    'parameterizedName': 'cacib.orders.OrderEventType',
    'shortDisplayName': 'OrderEventType',
    'longDisplayName': 'cacib.orders.OrderEventType'
  },
  'isCollection': false,
  'underlyingTypeParameters': [],
  'collectionType': null,
  'isScalar': true
};
