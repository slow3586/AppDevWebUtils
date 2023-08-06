var path = require('path');
const TerserPlugin = require("terser-webpack-plugin");
const StatoscopeWebpackPlugin = require('@statoscope/webpack-plugin').default;
const LodashModuleReplacementPlugin = require('lodash-webpack-plugin');

module.exports = {
    entry: './frontend/js/index.tsx',
    devtool: 'source-map',
    cache: true,
    mode: 'development',
    output: {
        path: __dirname,
        filename: './src/main/resources/static/built/bundle.js'
    },
    optimization: {
        minimize: true,
        minimizer: [new TerserPlugin()],
    },
    module: {
        rules: [
            {
                test: /\.(js|jsx|tsx|ts)$/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        'plugins': ['lodash']
                    }
                },
                exclude: /node_modules/,

            },
            {
                test: /\.(less)$/,
                use: [
                    'style-loader',
                    'css-loader',
                    'less-loader'
                ]
            },
            {
                test: /\.s[ac]ss|css$/i,
                use: [
                    "style-loader",
                    "css-loader",
                    "sass-loader",
                ],
            },
            {
                test: /\.png$/,
                use: ['url-loader?limit=100000']
            },
            {
                test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
                use: ['url-loader?limit=10000&mimetype=application/font-woff']
            },
            {
                test: /\.(ttf|otf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?|(jpg|gif)$/,
                use: ['file-loader']
            }
        ]
    },
    plugins: [
        //new StatoscopeWebpackPlugin(),
        new LodashModuleReplacementPlugin()
    ],
    resolve: {
        extensions: ['.tsx', '.ts', '.js', '.less'],
    },
};