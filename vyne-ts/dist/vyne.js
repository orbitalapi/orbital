"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const schemas_1 = require("./schemas");
class Vyne {
    constructor(url) {
        this.schemaService = new schemas_1.SchemaService(url);
    }
}
exports.Vyne = Vyne;
//# sourceMappingURL=vyne.js.map