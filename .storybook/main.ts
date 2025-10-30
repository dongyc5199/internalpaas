import type { StorybookConfig } from '@storybook/react-vite';
import { mergeConfig } from 'vite';

const config: StorybookConfig = {
  stories: [
    "../src/main/frontend/react-app/components/**/*.stories.@(js|jsx|mjs|ts|tsx)",
    "../src/main/frontend/react-app/components/**/*.mdx"
  ],
  addons: [
    "@storybook/addon-essentials",
    "@storybook/addon-interactions",
    "@storybook/addon-a11y"
  ],
  framework: {
    name: "@storybook/react-vite",
    options: {}
  },
  docs: {
    autodocs: "tag"
  },
  typescript: {
    reactDocgen: "react-docgen-typescript",
    reactDocgenTypescriptOptions: {
      shouldExtractLiteralValuesFromEnum: true,
      propFilter: (prop) => (prop.parent ? !/node_modules/.test(prop.parent.fileName) : true),
    },
  },
  async viteFinal(config) {
    return mergeConfig(config, {
      build: {
        target: 'es2020', // Support BigInt and modern features
      },
      esbuild: {
        target: 'es2020',
        supported: {
          'bigint': true,
          'top-level-await': true,
        },
      },
    });
  },
};
export default config;