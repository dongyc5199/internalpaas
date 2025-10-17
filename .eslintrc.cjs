module.exports = {
    root: true,
    env: {
        browser: true,
        es2021: true,
        node: false
    },
    parser: "@typescript-eslint/parser",
    parserOptions: {
        ecmaVersion: "latest",
        sourceType: "module"
    },
    plugins: ["@typescript-eslint", "prettier"],
    extends: [
        "eslint:recommended",
        "plugin:@typescript-eslint/recommended",
        "plugin:prettier/recommended"
    ],
    ignorePatterns: [
        "target/**",
        "node_modules/**",
        "src/main/resources/static/dist/**"
    ],
    rules: {
        "prettier/prettier": "error"
    }
};
