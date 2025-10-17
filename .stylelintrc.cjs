module.exports = {
    extends: ["stylelint-config-standard"],
    rules: {
        "color-function-notation": "modern",
        "alpha-value-notation": "percentage",
        "selector-class-pattern": null
    },
    ignoreFiles: [
        "node_modules/**/*",
        "target/**/*",
        "src/main/resources/static/dist/**/*"
    ]
};
