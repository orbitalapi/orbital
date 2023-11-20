const MonacoWebpackPlugin = require('monaco-editor-webpack-plugin');
const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const MONACO_DIR = path.join(__dirname, 'node_modules/monaco-editor');

// Workround to https://github.com/microsoft/monaco-editor/issues/3553#issuecomment-1432647208
module.exports = {
  module: {
    rules: [
      {
        test: /\.css$/,
        include: MONACO_DIR,
        use: [
          MiniCssExtractPlugin.loader,
          {
            loader: "css-loader"
          }
        ]
      },
      // {
      //     test: /\.ttf$/,
      //     include: MONACO_DIR,
      //     use: ['file-loader']
      // }
    ]
  },
  plugins: [
    new MonacoWebpackPlugin(),
  ]
};
