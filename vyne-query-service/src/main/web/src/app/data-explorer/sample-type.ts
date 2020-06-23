
export const sampleOrderEventType = {
  'name': {
    'fullyQualifiedName': 'bank.orders.OrderEventType',
    'parameters': [],
    'name': 'OrderEventType',
    'namespace': 'bank.orders',
    'parameterizedName': 'bank.orders.OrderEventType',
    'shortDisplayName': 'OrderEventType',
    'longDisplayName': 'bank.orders.OrderEventType'
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
        'bankY.orders.EntryType.Opened',
        'bankX.orders.EntryType.Opened'
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
        'bankX.orders.EntryType.WithHeld',
        'bankY.orders.EntryType.WithHeld'
      ],
      'typeDoc': ''
    }
  ],
  'sources': [
    {
      'name': 'bankY/Order.taxi',
      'version': '0.0.0',
      'content': 'Opened synonym of OrderEventType.Open',
      'id': 'bankY/Order.taxi:0.0.0'
    },
    {
      'name': 'bankX/orders/Order.taxi',
      'version': '0.0.0',
      'content': 'enum OrderEventType {\n   Open,\n   Filled,\n   Withheld\n}',
      'id': 'bankX/orders/Order.taxi:0.0.0'
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
  'fullyQualifiedName': 'bankX.orders.OrderEventType',
  'memberQualifiedName': {
    'fullyQualifiedName': 'bankX.orders.OrderEventType',
    'parameters': [],
    'name': 'OrderEventType',
    'namespace': 'bankX.orders',
    'parameterizedName': 'bankX.orders.OrderEventType',
    'shortDisplayName': 'OrderEventType',
    'longDisplayName': 'bankX.orders.OrderEventType'
  },
  'isCollection': false,
  'underlyingTypeParameters': [],
  'collectionType': null,
  'isScalar': true
};
