const path = require('path');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');

module.exports = {
    mode: 'development',
    entry: './public/assets/index.js',
    output: {
        path: path.resolve(__dirname, 'public/dist'),
        filename: 'bundle.js'
    },
    module: {
        rules: [
            {
                test: /\.(scss)$/i,
                // include: path.resolve(__dirname, './public/assets'),
                use: [
                    // { loader: 'style-loader' },
                    { loader: MiniCssExtractPlugin.loader },
                    { loader: 'css-loader' },
                    {
                        loader: 'postcss-loader',
                        options: {
                            postcssOptions: {
                                plugins: function() { return [ require('autoprefixer') ]; }
                            }
                        }
                    },
                    { loader: 'sass-loader' }
                ]
            },
            {
                test: /\.(js|jsx)$/i,
                // exclude: /(node_modules)/,
                include: path.resolve(__dirname, './public/assets'),
                use: [
                    { loader: 'babel-loader' }
                ]
            }
        ]
    },
    devServer: {
        port: 3000,
        watchFiles: [
            './public/assets/**'
        ]
    },
    plugins: [
        new MiniCssExtractPlugin()
    ]
};
