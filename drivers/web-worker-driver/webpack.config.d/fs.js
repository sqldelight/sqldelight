// Remove fs in the webpack config in order to build for front end
// https://github.com/webpack-contrib/css-loader/issues/447
config.resolve = {
    fallback: {
        fs: false,
        path: false,
        crypto: false,
    }
};
