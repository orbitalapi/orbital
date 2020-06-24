"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const request_promise_native_1 = __importDefault(require("request-promise-native"));
class SchemaSpec {
    constructor(name, version, defaultNamespace) {
        this.name = name;
        this.version = version;
        this.defaultNamespace = defaultNamespace;
    }
}
exports.SchemaSpec = SchemaSpec;
class SchemaImportRequest {
    constructor(spec, format, content) {
        this.spec = spec;
        this.format = format;
        this.content = content;
    }
}
exports.SchemaImportRequest = SchemaImportRequest;
class SchemaService {
    constructor(vyneUrl) {
        this.vyneUrl = vyneUrl;
    }
    submitSchema(schema) {
        let promise = request_promise_native_1.default.post(`${this.vyneUrl}/schemas`, {
            json: schema
        });
        return promise.promise();
    }
}
exports.SchemaService = SchemaService;
//# sourceMappingURL=schemas.js.map