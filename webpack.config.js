var path = require('path');
const TerserPlugin = require("terser-webpack-plugin");

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
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: /node_modules/,
            },
            {
                test: /\.less$/i,
                use: [
                    'style-loader',
                    'css-loader',
                    'less-loader'
                ],
                exclude: /node_modules/,
                include: /frontend/,
            }
        ]
    },
    resolve: {
        extensions: ['.tsx', '.ts', '.js', '.less'],
    },
};