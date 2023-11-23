const path = require('path');
// const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const MONACO_DIR = path.join(__dirname, 'node_modules/monaco-editor');
const VSCODE_DIR = path.join(__dirname, 'node_modules/vscode');

// Workround to https://github.com/microsoft/monaco-editor/issues/3553#issuecomment-1432647208
module.exports = {
  module: {
    rules: [
      {
        test: /\.css$/,
        include: [
          MONACO_DIR,
          VSCODE_DIR
        ],
        use: [
          // MiniCssExtractPlugin.loader,
          "css-loader",
        ]
      },
      // from https://github.com/TypeFox/monaco-languageclient-ng-example/blob/main/custom-webpack.config.js#L18C12-L21C14
      {
        test: /\.(mp3|wasm|ttf)$/i,
        type: 'asset/resource'
      }
    ],
    // this is required for loading .wasm (and other) files. For context, see https://stackoverflow.com/a/75252098 and https://github.com/angular/angular-cli/issues/24617
    parser: {
      javascript: {
        url: true
      }
    },
  },
  resolve: {
    extensions: ['.ts', '.js', '.json', '.ttf']
  },
  plugins: [
  ]
};
