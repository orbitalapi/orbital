export class typeData {
   public static TYPE_DATA = {
      "schemas": [
          {
              "types": [
                  {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
                          "name": "CreditCostRequest"
                      },
                      "attributes": {
                          "invoiceValue": {
                              "name": {
                                  "fullyQualifiedName": "polymer.creditInc.Money",
                                  "name": "Money"
                              },
                              "fullyQualifiedName": "polymer.creditInc.Money",
                              "constraints": [],
                              "collection": false
                          },
                          "industryCode": {
                              "name": {
                                  "fullyQualifiedName": "isic.uk.SIC2008",
                                  "name": "SIC2008"
                              },
                              "fullyQualifiedName": "isic.uk.SIC2008",
                              "constraints": [],
                              "collection": false
                          }
                      },
                      "modifiers": [
                          "PARAMETER_TYPE"
                      ],
                      "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
                      "scalar": false,
                      "parameterType": true
                  },
                  {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.Money",
                          "name": "Money"
                      },
                      "attributes": {
                          "currency": {
                              "name": {
                                  "fullyQualifiedName": "polymer.creditInc.Currency",
                                  "name": "Currency"
                              },
                              "fullyQualifiedName": "polymer.creditInc.Currency",
                              "constraints": [],
                              "collection": false
                          },
                          "value": {
                              "name": {
                                  "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
                                  "name": "MoneyAmount"
                              },
                              "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
                              "constraints": [],
                              "collection": false
                          }
                      },
                      "modifiers": [],
                      "fullyQualifiedName": "polymer.creditInc.Money",
                      "scalar": false,
                      "parameterType": false
                  },
                  {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.Currency",
                          "name": "Currency"
                      },
                      "attributes": {},
                      "modifiers": [],
                      "fullyQualifiedName": "polymer.creditInc.Currency",
                      "scalar": true,
                      "aliasForType" : "String",
                      "parameterType": false
                  },
                  {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
                          "name": "MoneyAmount"
                      },
                      "attributes": {},
                      "modifiers": [],
                      "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
                      "scalar": true,
                      "aliasForType" : "Decimal",
                      "parameterType": false
                  },
                  {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                          "name": "CreditCostResponse"
                      },
                      "attributes": {
                          "cost": {
                              "name": {
                                  "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                  "name": "CreditRiskCost"
                              },
                              "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                              "constraints": [],
                              "collection": false
                          }
                      },
                      "modifiers": [],
                      "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                      "scalar": false,
                      "parameterType": false
                  },
                  {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                          "name": "CreditRiskCost"
                      },
                      "attributes": {},
                      "modifiers": [],
                      "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                      "scalar": true,
                      "aliasForType" : "Decimal",
                      "parameterType": false
                  },
                  {
                      "name": {
                          "fullyQualifiedName": "isic.uk.SIC2008",
                          "name": "SIC2008"
                      },
                      "attributes": {},
                      "modifiers": [],
                      "fullyQualifiedName": "isic.uk.SIC2008",
                      "aliasForType" : "String",
                      "scalar": true,
                      "parameterType": false
                  }
              ],
              "services": [
                  {
                      "qualifiedName": "polymer.creditInc.creditMarkup.CreditCostService",
                      "operations": [
                          {
                              "name": "calculateCreditCosts",
                              "parameters": [
                                  {
                                      "type": {
                                          "name": {
                                              "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
                                              "name": "CreditCostRequest"
                                          },
                                          "attributes": {
                                              "invoiceValue": {
                                                  "name": {
                                                      "fullyQualifiedName": "polymer.creditInc.Money",
                                                      "name": "Money"
                                                  },
                                                  "fullyQualifiedName": "polymer.creditInc.Money",
                                                  "constraints": [],
                                                  "collection": false
                                              },
                                              "industryCode": {
                                                  "name": {
                                                      "fullyQualifiedName": "isic.uk.SIC2008",
                                                      "name": "SIC2008"
                                                  },
                                                  "fullyQualifiedName": "isic.uk.SIC2008",
                                                  "constraints": [],
                                                  "collection": false
                                              }
                                          },
                                          "modifiers": [
                                              "PARAMETER_TYPE"
                                          ],
                                          "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
                                          "scalar": false,
                                          "parameterType": true
                                      },
                                      "name": null,
                                      "metadata": [
                                          {
                                              "name": {
                                                  "fullyQualifiedName": "RequestBody",
                                                  "name": "RequestBody"
                                              },
                                              "params": {}
                                          }
                                      ],
                                      "constraints": []
                                  }
                              ],
                              "returnType": {
                                  "name": {
                                      "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                                      "name": "CreditCostResponse"
                                  },
                                  "attributes": {
                                      "cost": {
                                          "name": {
                                              "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                              "name": "CreditRiskCost"
                                          },
                                          "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                          "constraints": [],
                                          "collection": false
                                      }
                                  },
                                  "modifiers": [],
                                  "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                                  "scalar": false,
                                  "parameterType": false
                              },
                              "metadata": [
                                  {
                                      "name": {
                                          "fullyQualifiedName": "HttpOperation",
                                          "name": "HttpOperation"
                                      },
                                      "params": {
                                          "method": "POST",
                                          "url": "/costs"
                                      }
                                  }
                              ],
                              "contract": {
                                  "returnType": {
                                      "name": {
                                          "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                                          "name": "CreditCostResponse"
                                      },
                                      "attributes": {
                                          "cost": {
                                              "name": {
                                                  "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                                  "name": "CreditRiskCost"
                                              },
                                              "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                              "constraints": [],
                                              "collection": false
                                          }
                                      },
                                      "modifiers": [],
                                      "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                                      "scalar": false,
                                      "parameterType": false
                                  },
                                  "constraints": []
                              }
                          }
                      ],
                      "metadata": [
                          {
                              "name": {
                                  "fullyQualifiedName": "ServiceDiscoveryClient",
                                  "name": "ServiceDiscoveryClient"
                              },
                              "params": {
                                  "serviceName": "credit-pricing"
                              }
                          }
                      ]
                  }
              ],
              "links": [],
              "attributes": []
          }
      ],
      "types": [
          {
              "name": {
                  "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
                  "name": "CreditCostRequest"
              },
              "attributes": {
                  "invoiceValue": {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.Money",
                          "name": "Money"
                      },
                      "fullyQualifiedName": "polymer.creditInc.Money",
                      "constraints": [],
                      "collection": false
                  },
                  "industryCode": {
                      "name": {
                          "fullyQualifiedName": "isic.uk.SIC2008",
                          "name": "SIC2008"
                      },
                      "fullyQualifiedName": "isic.uk.SIC2008",
                      "constraints": [],
                      "collection": false
                  }
              },
              "modifiers": [
                  "PARAMETER_TYPE"
              ],
              "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
              "scalar": false,
              "parameterType": true
          },
          {
              "name": {
                  "fullyQualifiedName": "polymer.creditInc.Money",
                  "name": "Money"
              },
              "attributes": {
                  "currency": {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.Currency",
                          "name": "Currency"
                      },
                      "fullyQualifiedName": "polymer.creditInc.Currency",
                      "constraints": [],
                      "collection": false
                  },
                  "value": {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
                          "name": "MoneyAmount"
                      },
                      "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
                      "constraints": [],
                      "collection": false
                  }
              },
              "modifiers": [],
              "fullyQualifiedName": "polymer.creditInc.Money",
              "scalar": false,
              "parameterType": false
          },
          {
              "name": {
                  "fullyQualifiedName": "polymer.creditInc.Currency",
                  "name": "Currency"
              },
              "attributes": {},
              "modifiers": [],
              "fullyQualifiedName": "polymer.creditInc.Currency",
              "scalar": true,
              "aliasForType" : "String",
              "parameterType": false
          },
          {
              "name": {
                  "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
                  "name": "MoneyAmount"
              },
              "attributes": {},
              "modifiers": [],
              "fullyQualifiedName": "polymer.creditInc.MoneyAmount",
              "scalar": true,
              "aliasForType" : "Decimal",
              "parameterType": false
          },
          {
              "name": {
                  "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                  "name": "CreditCostResponse"
              },
              "attributes": {
                  "cost": {
                      "name": {
                          "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                          "name": "CreditRiskCost"
                      },
                      "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                      "constraints": [],
                      "collection": false
                  }
              },
              "modifiers": [],
              "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
              "scalar": false,
              "parameterType": false
          },
          {
              "name": {
                  "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                  "name": "CreditRiskCost"
              },
              "attributes": {},
              "modifiers": [],
              "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
              "scalar": true,
              "aliasForType" : "Decimal",
              "parameterType": false
          },
          {
              "name": {
                  "fullyQualifiedName": "isic.uk.SIC2008",
                  "name": "SIC2008"
              },
              "attributes": {},
              "modifiers": [],
              "fullyQualifiedName": "isic.uk.SIC2008",
              "scalar": true,
              "aliasForType" : "String",
              "parameterType": false
          }
      ],
      "links": [],
      "services": [
          {
              "qualifiedName": "polymer.creditInc.creditMarkup.CreditCostService",
              "operations": [
                  {
                      "name": "calculateCreditCosts",
                      "parameters": [
                          {
                              "type": {
                                  "name": {
                                      "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
                                      "name": "CreditCostRequest"
                                  },
                                  "attributes": {
                                      "invoiceValue": {
                                          "name": {
                                              "fullyQualifiedName": "polymer.creditInc.Money",
                                              "name": "Money"
                                          },
                                          "fullyQualifiedName": "polymer.creditInc.Money",
                                          "constraints": [],
                                          "collection": false
                                      },
                                      "industryCode": {
                                          "name": {
                                              "fullyQualifiedName": "isic.uk.SIC2008",
                                              "name": "SIC2008"
                                          },
                                          "fullyQualifiedName": "isic.uk.SIC2008",
                                          "constraints": [],
                                          "collection": false
                                      }
                                  },
                                  "modifiers": [
                                      "PARAMETER_TYPE"
                                  ],
                                  "fullyQualifiedName": "polymer.creditInc.CreditCostRequest",
                                  "scalar": false,
                                  "parameterType": true
                              },
                              "name": null,
                              "metadata": [
                                  {
                                      "name": {
                                          "fullyQualifiedName": "RequestBody",
                                          "name": "RequestBody"
                                      },
                                      "params": {}
                                  }
                              ],
                              "constraints": []
                          }
                      ],
                      "returnType": {
                          "name": {
                              "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                              "name": "CreditCostResponse"
                          },
                          "attributes": {
                              "cost": {
                                  "name": {
                                      "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                      "name": "CreditRiskCost"
                                  },
                                  "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                  "constraints": [],
                                  "collection": false
                              }
                          },
                          "modifiers": [],
                          "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                          "scalar": false,
                          "parameterType": false
                      },
                      "metadata": [
                          {
                              "name": {
                                  "fullyQualifiedName": "HttpOperation",
                                  "name": "HttpOperation"
                              },
                              "params": {
                                  "method": "POST",
                                  "url": "/costs"
                              }
                          }
                      ],
                      "contract": {
                          "returnType": {
                              "name": {
                                  "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                                  "name": "CreditCostResponse"
                              },
                              "attributes": {
                                  "cost": {
                                      "name": {
                                          "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                          "name": "CreditRiskCost"
                                      },
                                      "fullyQualifiedName": "polymer.creditInc.CreditRiskCost",
                                      "constraints": [],
                                      "collection": false
                                  }
                              },
                              "modifiers": [],
                              "fullyQualifiedName": "polymer.creditInc.CreditCostResponse",
                              "scalar": false,
                              "parameterType": false
                          },
                          "constraints": []
                      }
                  }
              ],
              "metadata": [
                  {
                      "name": {
                          "fullyQualifiedName": "ServiceDiscoveryClient",
                          "name": "ServiceDiscoveryClient"
                      },
                      "params": {
                          "serviceName": "credit-pricing"
                      }
                  }
              ]
          }
      ],
      "attributes": []
  }
}
